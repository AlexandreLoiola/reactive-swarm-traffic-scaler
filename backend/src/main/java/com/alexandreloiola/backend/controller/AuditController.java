package com.alexandreloiola.backend.controller;

import com.sun.management.OperatingSystemMXBean;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Map;

@Path("/api")
public class AuditController {

    private final OperatingSystemMXBean osBean =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    @GET
    @Path("/audit")
    public Map<String, Object> audit() {
        return Map.of(
                "container", System.getenv().getOrDefault("HOSTNAME", "unknown"),
                "time", Instant.now().toString()
        );
    }

    @GET
    @Path("/cpu")
    public Map<String, Object> cpu() {
        return Map.of(
                "processCpuLoad", round(osBean.getProcessCpuLoad()),
                "systemCpuLoad", round(osBean.getSystemCpuLoad()),
                "availableProcessors", osBean.getAvailableProcessors()
        );
    }

    private double round(double value) {
        return value < 0 ? 0 : Math.round(value * 100.0) / 100.0;
    }
}

