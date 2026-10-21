package io.github.kxyr0.echoplayer.api.spark;

/**
 * Immutable Spark projection for public APIs and synchronization.
 */
public record PlayerSparkSnapshot(int current, int maximum) {
    public PlayerSparkSnapshot {
        new PlayerSpark(current, maximum);
    }

    public static PlayerSparkSnapshot from(PlayerSpark spark) {
        return new PlayerSparkSnapshot(spark.current(), spark.maximum());
    }

    public PlayerSpark toSpark() {
        return new PlayerSpark(current, maximum);
    }
}
