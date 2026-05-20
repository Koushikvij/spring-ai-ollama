package com.koushik.course_catalog.common.saga;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

    public void execute(String sagaName, List<SagaStep> steps, SagaContext context) {
        List<SagaStep> completed = new ArrayList<>();
        try {
            for (SagaStep step : steps) {
                log.debug("Saga [{}] executing step: {}", sagaName, step.name());
                step.run(context);
                completed.add(step);
            }
            log.info("Saga [{}] completed successfully", sagaName);
        } catch (Exception ex) {
            log.error("Saga [{}] failed, running compensation", sagaName, ex);
            Collections.reverse(completed);
            for (SagaStep step : completed) {
                try {
                    log.debug("Saga [{}] compensating step: {}", sagaName, step.name());
                    step.compensate(context);
                } catch (Exception compensationEx) {
                    log.error("Saga [{}] compensation failed for step: {}", sagaName, step.name(), compensationEx);
                }
            }
            throw new SagaException("Saga [" + sagaName + "] failed: " + ex.getMessage(), ex);
        }
    }
}
