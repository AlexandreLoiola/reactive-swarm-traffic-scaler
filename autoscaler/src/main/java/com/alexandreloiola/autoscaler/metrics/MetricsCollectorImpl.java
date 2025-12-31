package com.alexandreloiola.autoscaler.metrics;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
@Log4j2
public class MetricsCollectorImpl implements MetricsCollector {

    @Value("${autoscaler.metrics.nginx.status-url}")
    private String nginxStatusUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private double lastCount = 0;
    private long lastTimestamp = System.currentTimeMillis();

    @Override
    public synchronized double collectAverageRps() {
        try {
            String status = restTemplate.getForObject(nginxStatusUrl, String.class);

            if (status == null) {
                log.warn("Received empty response from Nginx status endpoint");
                return 0;
            }

            double totalRequests = parseRequests(status);

            long now = System.currentTimeMillis();
            double elapsedSeconds = (now - lastTimestamp) / 1000.0;

            if (elapsedSeconds <= 0) {
                log.debug("Invalid elapsed time while calculating RPS. Returning 0");
                return 0;
            }

            double rps = (totalRequests - lastCount) / elapsedSeconds;

            lastCount = totalRequests;
            lastTimestamp = now;

            double finalRps = Math.max(0, rps);
            log.debug("Calculated RPS: {}", finalRps);

            return finalRps;
        } catch (ResourceAccessException ex) {
            log.warn("Unable to reach Nginx status endpoint. Network may not be ready. Using fallback RPS", ex);
            return lastCount > 0 ? (lastCount / 100.0) : 0;
        } catch (Exception ex) {
            log.error("Failed to collect Nginx metrics. Returning RPS=0", ex);
            return 0;
        }
    }

    private double parseRequests(String status) {
        for (String line : status.split("\n")) {
            line = line.trim();
            if (line.matches("\\d+\\s+\\d+\\s+\\d+")) {
                String[] parts = line.split("\\s+");
                return Double.parseDouble(parts[2]);
            }
        }

        throw new IllegalStateException("Unable to parse Nginx stub_status response");
    }
}