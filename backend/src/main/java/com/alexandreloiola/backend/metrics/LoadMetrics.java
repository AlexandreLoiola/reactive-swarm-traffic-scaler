package com.alexandreloiola.backend.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Component
public class LoadMetrics {

    private final AtomicInteger activeRequests = new AtomicInteger();
    private final Counter totalRequests;
    private final Timer processingTimer;

    public LoadMetrics(MeterRegistry registry) {
        this.totalRequests = Counter.builder("app.requests.total")
                .description("Total requests processed")
                .register(registry);

        this.processingTimer = Timer.builder("app.requests.latency")
                .description("Request processing latency")
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
