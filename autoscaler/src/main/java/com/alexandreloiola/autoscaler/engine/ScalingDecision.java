package com.alexandreloiola.autoscaler.engine;

public record ScalingDecision(
    ScalingDecisionEnum action,
    int instanceDelta
) {
    public boolean hasEffect() {
        return action != ScalingDecisionEnum.DO_NOTHING
                && instanceDelta > 0;
    }
}
