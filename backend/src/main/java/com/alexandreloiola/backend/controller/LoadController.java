package com.alexandreloiola.backend.controller;

import com.alexandreloiola.backend.service.LoadService;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.Map;
import java.util.Random;

@Path("/api")
public class LoadController {

    @Inject
    LoadService loadService;

    private final Random random = new Random();

    @GET
    @Path("/load")
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    public Map<String, Object> load() {
        loadService.simulateWork();
        return Map.of(
                "orderId", random.nextInt(10000),
                "timestamp", Instant.now().toString()
        );
    }
}