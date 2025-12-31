package com.alexandreloiola.autoscaler.scheduler;

import com.alexandreloiola.autoscaler.engine.CooldownManager;
import com.alexandreloiola.autoscaler.engine.ScalingDecision;
import com.alexandreloiola.autoscaler.metrics.MetricsCollector;
import com.alexandreloiola.autoscaler.engine.ScalingDecisionEnum;
import com.alexandreloiola.autoscaler.engine.ScalingPolicy;
import com.alexandreloiola.autoscaler.docker.DockerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.alexandreloiola.autoscaler.engine.ScalingDecisionEnum.SCALE_DOWN;
import static com.alexandreloiola.autoscaler.engine.ScalingDecisionEnum.SCALE_UP;

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
            if (!cooldown.canScale()) {
                log.warn("Cooldown active. Skipping scaling evaluation");
                return;
            }
            double avgRps = metrics.collectAverageRps();
            int instances = docker.countBackendInstances();

            ScalingDecision decision = policy.evaluate(avgRps, instances);

            executeScalingDecision(decision);
        } catch (Exception ex) {
            log.error("Error during autoscaler evaluation loop (ignored)", ex);
        }
    }

    private void executeScalingDecision(ScalingDecision decision) {

        if (!decision.hasEffect()) {
            log.debug("No scaling action need to be executed");
            return;
        }

        log.info("Executing scaling decision: action={} delta={}",
                decision.action(), decision.instanceDelta()
        );

        switch (decision.action()) {

            case SCALE_UP ->
                    docker.scaleUp(decision.instanceDelta());

            case SCALE_DOWN ->
                    docker.scaleDown(decision.instanceDelta());
        }

        cooldown.markScaled();
        log.debug("Cooldown timer reset after scaling action");
    }
}