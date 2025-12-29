package com.alexandreloiola.backend.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Component
public class RequestMetrics {

    private final AtomicInteger activeRequests;

    private final Counter totalRequests;
    private final Timer requestLatency;

    public RequestMetrics(MeterRegistry registry) {
        this.activeRequests = new AtomicInteger(0);

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