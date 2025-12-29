package com.alexandreloiola.autoscaler.scheduler;

import com.alexandreloiola.autoscaler.engine.CooldownManager;
import com.alexandreloiola.autoscaler.metrics.MetricsCollector;
import com.alexandreloiola.autoscaler.engine.ScalingDecisionEnum;
import com.alexandreloiola.autoscaler.engine.ScalingPolicy;
import com.alexandreloiola.autoscaler.docker.DockerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@EnableScheduling
@Log4j2
public class AutoscalerScheduler {

    private final MetricsCollector metrics;
    private final DockerService docker;
    private final ScalingPolicy policy;
    private final CooldownManager cooldown;

    @Scheduled(fixedDelay = 10_000)
    public void evaluate() {
        try {
            log.info("Starting autoscaling evaluation cycle");

            if (!cooldown.canScale()) {
                log.warn("Cooldown active. Skipping scaling evaluation");
                return;
            }

            double avgRps = metrics.collectAverageRps();
            int instances = docker.countBackendInstances();

            ScalingDecisionEnum decision = policy.evaluate(avgRps, instances);

            switch (decision) {
                case SCALE_UP -> docker.scaleUp();
                case SCALE_DOWN -> docker.scaleDown();
                default -> {}
            }

            if (decision != ScalingDecisionEnum.DO_NOTHING) {
                cooldown.markScaled();
                log.debug("Cooldown timer reset after scaling action");
            }
        } catch (Exception ex) {
            log.error("Error during autoscaler evaluation loop (ignored)", ex);
        }
    }
}