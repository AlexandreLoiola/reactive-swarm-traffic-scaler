package com.alexandreloiola.backend.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.inject.Singleton;

@Singleton
public class LoadMetrics {

    private final Counter errors;
    private final Counter requests;
    private final Timer executionTime;

    public LoadMetrics(MeterRegistry registry) {
        this.requests = Counter.builder("app_load_requests_total")
                .description("Total load simulation requests")
                .register(registry);

        this.errors = Counter.builder("app_load_errors_total")
                .description("Total load simulation errors")
                .register(registry);

        this.executionTime = Timer.builder("app_load_execution_time")
                .description("Load simulation execution time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void incRequests() {
        requests.increment();
    }

    public void incErrors() {
        errors.increment();
    }

    public Timer.Sample startTimer(MeterRegistry registry) {
        return Timer.start(registry);
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(executionTime);
    }
}
