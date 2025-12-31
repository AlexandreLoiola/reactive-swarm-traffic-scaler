package com.alexandreloiola.autoscaler.engine;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class ScalingPolicy {

    @Value("${autoscaler.capacity.max-rps-per-instance:100}")
    private double maxSafeRpsPerInstance;

    @Value("${autoscaler.target.utilization:0.7}")
    private double targetUtilization;

    @Value("${autoscaler.scale-up.aggressiveness:2.0}")
    private double scaleUpAggressiveness;

    @Value("${autoscaler.scale-down.conservativeness:0.3}")
    private double scaleDownConservativeness;

    @Value("${autoscaler.scale-down.dead-zone:0.15}")
    private double scaleDownDeadZone;

    @Value("${autoscaler.scaling.min-instances:1}")
    private int minInstances;

    @Value("${autoscaler.scaling.max-instances:10}")
    private int maxInstances;

    @Value("${autoscaler.scale-down.damping-factor:2.0}")
    private double scaleDownDampingFactor;

    private int consecutiveLowUtilizationSamples = 0;

    @Value("${autoscaler.scale-down.stability-threshold:5}")
    private int requiredStableSamples;

    public ScalingDecision evaluate(double totalAverageRps, int currentInstances) {
        if (currentInstances <= 0) {
            log.warn("Invalid instance count: {}", currentInstances);
            return noScalingDecision();
        }

        double currentUtilization = calculateCurrentUtilization(totalAverageRps, currentInstances);
        double utilizationDelta = currentUtilization - targetUtilization;

        if (shouldScaleUp(utilizationDelta, currentInstances)) {
            consecutiveLowUtilizationSamples = 0;
            return buildScaleUpDecision(utilizationDelta, currentInstances, currentUtilization);
        }

        if (shouldScaleDown(utilizationDelta, currentInstances)) {
            consecutiveLowUtilizationSamples++;

            if (consecutiveLowUtilizationSamples >= requiredStableSamples) {
                return buildScaleDownDecision(utilizationDelta, currentInstances, currentUtilization);
            }
            log.debug("Scale down suppressed: low utilization detected but not stable yet ({}/{})",
                    consecutiveLowUtilizationSamples, requiredStableSamples
            );
        } else {
            consecutiveLowUtilizationSamples = 0;
        }

        return noScalingDecision();
    }

    private boolean shouldScaleUp(double utilizationDelta, int currentInstances) {
        return utilizationDelta > 0 && currentInstances < maxInstances;
    }

    private boolean shouldScaleDown(double utilizationDelta, int currentInstances) {
        return utilizationDelta < -scaleDownDeadZone && currentInstances > minInstances;
    }

    private ScalingDecision buildScaleUpDecision(
            double utilizationDelta,
            int currentInstances,
            double currentUtilization
    ) {
        int instancesToAdd = calculateInstancesToAdd(utilizationDelta, currentInstances);
        log.info("Scale UP: utilization={} target={} delta={} adding={} instances",
                round(currentUtilization), targetUtilization, round(utilizationDelta), instancesToAdd
        );
        return new ScalingDecision(ScalingDecisionEnum.SCALE_UP, instancesToAdd);
    }

    private ScalingDecision buildScaleDownDecision(
            double utilizationDelta,
            int currentInstances,
            double currentUtilization
    ) {
        int instancesToRemove = calculateInstancesToRemove(utilizationDelta, currentInstances);
        log.info("Scale DOWN: utilization={} target={} delta={} removing={} instances",
                round(currentUtilization), targetUtilization, round(utilizationDelta), instancesToRemove
        );
        return new ScalingDecision(ScalingDecisionEnum.SCALE_DOWN, instancesToRemove);
    }

    private double calculateCurrentUtilization(double totalAverageRps, int currentInstances) {
        double rpsPerInstance = totalAverageRps / currentInstances;
        return rpsPerInstance / maxSafeRpsPerInstance;
    }

    private int calculateInstancesToAdd(double utilizationDelta, int currentInstances) {
        int instancesToAdd = (int) Math.ceil(scaleUpAggressiveness * utilizationDelta * currentInstances);
        return Math.min(instancesToAdd, maxInstances - currentInstances);
    }

    private int calculateInstancesToRemove(double utilizationDelta, int currentInstances) {
        double underUtilization = Math.abs(utilizationDelta);
        double dampedFactor = Math.pow(underUtilization, 1.0 / scaleDownDampingFactor);
        int instancesToRemove = (int) Math.floor(dampedFactor * scaleDownConservativeness * currentInstances);

        return Math.min(Math.max(instancesToRemove, 1), currentInstances - minInstances);
    }

    private ScalingDecision noScalingDecision() {
        return new ScalingDecision(ScalingDecisionEnum.DO_NOTHING, 0);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}