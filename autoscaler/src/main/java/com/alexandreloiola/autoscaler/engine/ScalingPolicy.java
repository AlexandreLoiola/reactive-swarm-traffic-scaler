package com.alexandreloiola.autoscaler.engine;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class ScalingPolicy {

    @Value("${autoscaler.scaling.scale-up-rps-threshold:50}")
    private double scaleUpRpsThreshold;

    @Value("${autoscaler.scaling.scale-down-rps-threshold:15}")
    private double scaleDownRpsThreshold;

    @Value("${autoscaler.scaling.min-instances:1}")
    private int minInstances;

    @Value("${autoscaler.scaling.max-instances:10}")
    private int maxInstances;

    public ScalingDecisionEnum evaluate(double avgRps, int instances) {

        if (instances <= 0) {
            log.warn("Invalid instance count: {}", instances);
            return ScalingDecisionEnum.DO_NOTHING;
        }

        double rpsPerInstance = avgRps / instances;

        if (rpsPerInstance > scaleUpRpsThreshold && instances < maxInstances) {
            log.info(
                    "Scale up triggered. rpsPerInstance={} exceeds threshold={} (totalRps={}, instances={})",
                    round(rpsPerInstance),
                    scaleUpRpsThreshold,
                    round(avgRps),
                    instances
            );
            return ScalingDecisionEnum.SCALE_UP;
        }

        if (rpsPerInstance < scaleDownRpsThreshold && instances > minInstances) {
            log.info(
                    "Scale down triggered. rpsPerInstance={} below threshold={} (totalRps={}, instances={})",
                    round(rpsPerInstance),
                    scaleDownRpsThreshold,
                    round(avgRps),
                    instances
            );
            return ScalingDecisionEnum.SCALE_DOWN;
        }

        log.debug(
                "No scaling action required. rpsPerInstance={}, totalRps={}, instances={}",
                round(rpsPerInstance),
                round(avgRps),
                instances
        );
        return ScalingDecisionEnum.DO_NOTHING;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}