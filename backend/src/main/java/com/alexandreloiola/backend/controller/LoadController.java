package com.alexandreloiola.backend.controller;

import com.alexandreloiola.backend.metrics.RequestMetrics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LoadController {

    private final RequestMetrics metrics;

    public LoadController(RequestMetrics metrics) {
        this.metrics = metrics;
    }

    @GetMapping("/load")
    public String load() {
        return metrics.record(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "OK";
        });
    }
}
