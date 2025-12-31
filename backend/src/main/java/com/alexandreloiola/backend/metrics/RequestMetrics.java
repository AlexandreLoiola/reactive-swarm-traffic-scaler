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
public class RequestMetrics {

    private final AtomicInteger activeRequests = new AtomicInteger(0);
    private final Counter totalRequests;
    private final Timer requestLatency;

    @Inject
    public RequestMetrics(MeterRegistry registry) {
        this.totalRequests = Counter.builder("app.requests.total")
                .description("Total number of processed requests")
                .register(registry);

        this.requestLatency = Timer.builder("app.requests.latency")
                .description("Request processing latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        Gauge.builder("app.requests.active", activeRequests, AtomicInteger::get)
                .description("Number of active requests")
                .register(registry);
    }

    public <T> T record(Supplier<T> supplier) {
        activeRequests.incrementAndGet();
        try {
            return requestLatency.record(supplier);
        } finally {
            totalRequests.increment();
            activeRequests.decrementAndGet();
        }
    }
}