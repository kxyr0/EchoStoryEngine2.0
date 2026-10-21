package io.github.kxyr0.echoplayer.testmod;

import io.github.kxyr0.echoplayer.api.spark.PlayerSpark;
import io.github.kxyr0.echoplayer.core.spark.PlayerSparkMutation;

/**
 * Development-only hooks for manual acceptance scenarios.
 */
public final class EchoPlayerTestmodHooks {
    private final PlayerSparkMutation sparkMutation = new PlayerSparkMutation();

    public PlayerSpark spendForManualCheck(int current, int maximum, int amount) {
        return sparkMutation.spend(new PlayerSpark(current, maximum), amount).spark();
    }
}
