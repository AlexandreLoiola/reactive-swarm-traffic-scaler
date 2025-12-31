package com.alexandreloiola.backend.controller;

import com.alexandreloiola.backend.metrics.RequestMetrics;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;


@Path("/api")
public class MockController {

    @Inject
    RequestMetrics metrics;

    private final Random random = new Random();

    @GET
    @Path("/load")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> processOrder() {
        return metrics.record(() -> {
            // Simula leitura do DB
            List<String> items = List.of("item1", "item2", "item3");
            int processedItems = random.nextInt(items.size()) + 1;

            // Simula lógica de negócio
            boolean discountApplied = processedItems > 2;

            // Simula latência de rede / serviço externo
            try {
                Thread.sleep(50 + random.nextInt(100));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return Map.of(
                    "orderId", random.nextInt(10000),
                    "processedItems", processedItems,
                    "discountApplied", discountApplied,
                    "timestamp", Instant.now().toString()
            );
        });
    }
}