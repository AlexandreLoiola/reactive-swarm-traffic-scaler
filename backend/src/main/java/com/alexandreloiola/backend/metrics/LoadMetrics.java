package com.alexandreloiola.backend.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Singleton
public class LoadMetrics {

    private final AtomicInteger activeRequests = new AtomicInteger();
    private final Counter totalRequests;
    private final Timer processingTimer;

    @Inject
    public LoadMetrics(MeterRegistry registry) {
        this.totalRequests = Counter.builder("app.load.total")
                .description("Total requests processed for load")
                .register(registry);

        this.processingTimer = Timer.builder("app.load.latency")
                .description("Request processing latency for load")
                .register(registry);

        Gauge.builder("app.load.pressure", activeRequests,
                        ar -> Math.min(ar.get() / 100.0, 1.0))
                .description("Relative load pressure")
                .register(registry);
    }

    public <T> T record(Supplier<T> supplier) {
        activeRequests.incrementAndGet();
        try {
            return processingTimer.record(supplier);
        } finally {
            totalRequests.increment();
            activeRequests.decrementAndGet();
        }
    }
}