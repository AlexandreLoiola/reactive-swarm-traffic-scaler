package com.alexandreloiola.backend.controller;

import com.sun.management.OperatingSystemMXBean;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuditController {

    private final Environment env;
    private final OperatingSystemMXBean osBean =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    public AuditController(Environment env) {
        this.env = env;
    }

    @GetMapping("/audit")
    public Map<String, Object> audit() {
        return Map.of(
                "container", env.getProperty("HOSTNAME", "unknown"),
                "time", Instant.now().toString()
        );
    }

    @GetMapping("/cpu")
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

