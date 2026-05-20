package com.koushik.course_catalog.common.saga;

@FunctionalInterface
public interface SagaStep {

    void run(SagaContext context) throws Exception;

    default String name() {
        return getClass().getSimpleName();
    }

    default void compensate(SagaContext context) {
        // optional rollback for this step
    }
}
