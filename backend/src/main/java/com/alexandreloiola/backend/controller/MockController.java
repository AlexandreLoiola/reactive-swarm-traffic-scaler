package com.alexandreloiola.backend.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.Map;
import java.util.Random;


@Path("/api")
public class MockController {

    private final Random random = new Random();

    @GET
    @Path("/load")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> load() {
        simulateWork();
        return Map.of(
                "orderId", random.nextInt(10000),
                "timestamp", Instant.now().toString()
        );
    }

    private void simulateWork() {
        try {
            Thread.sleep(300 + random.nextInt(100));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}