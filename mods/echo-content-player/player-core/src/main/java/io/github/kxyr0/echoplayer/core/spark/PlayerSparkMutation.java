package io.github.kxyr0.echoplayer.core.spark;

import io.github.kxyr0.echoplayer.api.spark.PlayerSpark;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Atomic Spark value transformations.
 */
public final class PlayerSparkMutation {
    public SparkMutationResult set(PlayerSpark spark, int current) {
        Objects.requireNonNull(spark, "spark");
        if (current < 0 || current > spark.maximum()) {
            return new SparkMutationResult(spark, SparkMutationStatus.INVALID_AMOUNT);
        }
        return new SparkMutationResult(new PlayerSpark(current, spark.maximum()), SparkMutationStatus.SUCCESS);
    }

    public SparkMutationResult restore(PlayerSpark spark, int amount) {
        Objects.requireNonNull(spark, "spark");
        if (amount < 0) {
            return new SparkMutationResult(spark, SparkMutationStatus.INVALID_AMOUNT);
        }
        long next = (long) spark.current() + amount;
        return new SparkMutationResult(new PlayerSpark((int) Math.min(next, spark.maximum()),
                spark.maximum()), SparkMutationStatus.SUCCESS);
    }

    public SparkMutationResult spend(PlayerSpark spark, int amount) {
        Objects.requireNonNull(spark, "spark");
        if (amount < 0) {
            return new SparkMutationResult(spark, SparkMutationStatus.INVALID_AMOUNT);
        }
        if (spark.current() < amount) {
            return new SparkMutationResult(spark, SparkMutationStatus.INSUFFICIENT_SPARK);
        }
        return new SparkMutationResult(new PlayerSpark(spark.current() - amount,
                spark.maximum()), SparkMutationStatus.SUCCESS);
    }

    public SparkMutationResult trySpend(PlayerSpark spark, int amount) {
        return spend(spark, amount);
    }

    public SparkMutationResult setMaximum(PlayerSpark spark, int maximum, MaximumSparkPolicy policy) {
        Objects.requireNonNull(spark, "spark");
        Objects.requireNonNull(policy, "policy");
        if (maximum <= 0 || maximum > PlayerSpark.HARD_MAXIMUM) {
            return new SparkMutationResult(spark, SparkMutationStatus.INVALID_AMOUNT);
        }
        int current = policy == MaximumSparkPolicy.CLAMP_CURRENT
                ? Math.min(spark.current(), maximum)
                : preservePercentage(spark, maximum);
        return new SparkMutationResult(new PlayerSpark(current, maximum), SparkMutationStatus.SUCCESS);
    }

    private int preservePercentage(PlayerSpark spark, int maximum) {
        BigDecimal ratio = BigDecimal.valueOf(spark.current())
                .divide(BigDecimal.valueOf(spark.maximum()), 8, RoundingMode.HALF_UP);
        return ratio.multiply(BigDecimal.valueOf(maximum)).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }
}
