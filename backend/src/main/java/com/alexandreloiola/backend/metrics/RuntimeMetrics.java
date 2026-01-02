package com.alexandreloiola.backend.metrics;

import com.sun.management.OperatingSystemMXBean;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Singleton;

import java.lang.management.ManagementFactory;

@Singleton
public class RuntimeMetrics {

    private final OperatingSystemMXBean osBean =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    public RuntimeMetrics(MeterRegistry registry) {

        Gauge.builder("app_process_cpu_load", osBean,
                        b -> safe(b.getProcessCpuLoad()))
                .description("CPU usage of the JVM process")
                .register(registry);

        Gauge.builder("app_system_cpu_load", osBean,
                        b -> safe(b.getCpuLoad()))
                .description("System CPU usage")
                .register(registry);

        Gauge.builder("app_available_processors", osBean,
                        OperatingSystemMXBean::getAvailableProcessors)
                .description("Available processors")
                .register(registry);
    }

    private double safe(double value) {
        return value < 0 ? 0 : value;
    }
}