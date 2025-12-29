package com.alexandreloiola.autoscaler.engine;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@Log4j2
public class CooldownManager {

    @Value("${autoscaler.cooldown.duration-seconds}")
    private long cooldownSeconds;

    private Instant lastScaling = Instant.MIN;

    public boolean canScale() {
        Instant now = Instant.now();
        Instant nextAllowedScaling = lastScaling.plusSeconds(cooldownSeconds);

        boolean canScale = now.isAfter(nextAllowedScaling);

        if (!canScale) {
            long remainingSeconds = Duration.between(now, nextAllowedScaling).toSeconds();
            log.debug("Cooldown active. Next scaling allowed in {} seconds", remainingSeconds);
        }
        return canScale;
    }

    public void markScaled() {
        lastScaling = Instant.now();
        log.debug("Scaling action recorded. Cooldown timer started");
    }
}
