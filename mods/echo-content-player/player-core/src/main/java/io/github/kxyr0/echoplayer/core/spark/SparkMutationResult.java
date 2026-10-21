package io.github.kxyr0.echoplayer.core.spark;

import io.github.kxyr0.echoplayer.api.spark.PlayerSpark;
import java.util.Objects;

/**
 * Immutable result for a Spark mutation.
 */
public record SparkMutationResult(PlayerSpark spark, SparkMutationStatus status) {
    public SparkMutationResult {
        spark = Objects.requireNonNull(spark, "spark");
        status = Objects.requireNonNull(status, "status");
    }

    public boolean changed(PlayerSpark before) {
        return !spark.equals(before);
    }
}
