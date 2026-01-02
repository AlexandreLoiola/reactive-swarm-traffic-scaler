package com.alexandreloiola.backend.service;

import com.alexandreloiola.backend.metrics.LoadMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Random;

@ApplicationScoped
public class LoadService {

    private final Random random = new Random();

    @Inject
    LoadMetrics metrics;

    @Inject
    MeterRegistry registry;

    public void simulateWork() {
        metrics.incRequests();
        Timer.Sample sample = metrics.startTimer(registry);

        try {
            simulateCpu();
            simulateIo();
        } catch (Exception e) {
            metrics.incErrors();
            throw e;
        } finally {
            metrics.stopTimer(sample);
        }
    }

    private void simulateCpu() {
        long end = System.nanoTime() +
                (200_000_000L + random.nextInt(100_000_000));

        while (System.nanoTime() < end) {
            Math.log(random.nextDouble() + 1);
        }
    }

    private void simulateIo() {
        try {
            Thread.sleep(100 + random.nextInt(100));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
