package io.github.kxyr0.echoplayersleep.core.scenario;

import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepScenarioId;
import io.github.kxyr0.echoplayersleep.api.policy.PlayerSleepControlMode;
import io.github.kxyr0.echoplayersleep.api.policy.PlayerSleepRecoveryPolicy;
import io.github.kxyr0.echoplayersleep.api.policy.PlayerSleepReturnMode;
import java.util.Objects;

public record PlayerSleepScenarioDefinition(
        PlayerSleepScenarioId scenarioId,
        String startNode,
        boolean requireNight,
        PlayerSleepBedRule bedRule,
        PlayerSleepControlMode controlMode,
        PlayerSleepReturnMode returnMode,
        PlayerSleepRecoveryPolicy recoveryPolicy,
        long revision) {
    public PlayerSleepScenarioDefinition {
        scenarioId = Objects.requireNonNull(scenarioId, "scenarioId");
        startNode = requireText(startNode, "startNode");
        bedRule = Objects.requireNonNull(bedRule, "bedRule");
        controlMode = Objects.requireNonNull(controlMode, "controlMode");
        returnMode = Objects.requireNonNull(returnMode, "returnMode");
        recoveryPolicy = Objects.requireNonNull(recoveryPolicy, "recoveryPolicy");
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checked;
    }
}
