package com.koushik.course_catalog.common.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SagaOrchestratorTest {

    private SagaOrchestrator orchestrator;
    private SagaContext context;

    @BeforeEach
    void setUp() {
        orchestrator = new SagaOrchestrator();
        context = new SagaContext();
    }

    @Test
    void execute_runsAllStepsWhenSuccessful() {
        List<String> order = new ArrayList<>();
        orchestrator.execute("TestSaga", List.of(
                step("Step1", () -> order.add("run1")),
                step("Step2", () -> order.add("run2"))), context);

        assertThat(order).containsExactly("run1", "run2");
    }

    @Test
    void execute_compensatesCompletedStepsInReverseOrderOnFailure() {
        List<String> events = new ArrayList<>();

        assertThatThrownBy(() -> orchestrator.execute("FailingSaga", List.of(
                step("Step1",
                        () -> events.add("run1"),
                        () -> events.add("comp1")),
                step("Step2",
                        () -> events.add("run2"),
                        () -> events.add("comp2")),
                step("Step3",
                        () -> {
                            events.add("run3");
                            throw new RuntimeException("step failed");
                        },
                        () -> events.add("comp3"))), context))
                .isInstanceOf(SagaException.class)
                .hasMessageContaining("FailingSaga");

        assertThat(events).containsExactly("run1", "run2", "run3", "comp2", "comp1");
    }

    private SagaStep step(String name, Runnable run, Runnable compensate) {
        return new SagaStep() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public void run(SagaContext ctx) {
                run.run();
            }

            @Override
            public void compensate(SagaContext ctx) {
                if (compensate != null) {
                    compensate.run();
                }
            }
        };
    }

    private SagaStep step(String name, Runnable run) {
        return step(name, run, null);
    }
}
