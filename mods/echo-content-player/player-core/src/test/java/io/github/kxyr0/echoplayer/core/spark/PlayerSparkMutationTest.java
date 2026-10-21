package io.github.kxyr0.echoplayer.core.spark;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echoplayer.api.spark.PlayerSpark;
import org.junit.jupiter.api.Test;

final class PlayerSparkMutationTest {
    private final PlayerSparkMutation mutation = new PlayerSparkMutation();

    @Test
    void spendSucceedsAtomically() {
        SparkMutationResult result = mutation.spend(new PlayerSpark(100, 100), 30);

        assertEquals(SparkMutationStatus.SUCCESS, result.status());
        assertEquals(new PlayerSpark(70, 100), result.spark());
    }

    @Test
    void insufficientSpendDoesNotPartiallyMutate() {
        PlayerSpark before = new PlayerSpark(70, 100);

        SparkMutationResult result = mutation.spend(before, 100);

        assertEquals(SparkMutationStatus.INSUFFICIENT_SPARK, result.status());
        assertEquals(before, result.spark());
    }

    @Test
    void restoreClampsAndMaximumPolicyWorks() {
        PlayerSpark restored = mutation.restore(new PlayerSpark(95, 100), 20).spark();
        PlayerSpark clamped = mutation.setMaximum(restored, 50, MaximumSparkPolicy.CLAMP_CURRENT).spark();
        PlayerSpark percent = mutation.setMaximum(new PlayerSpark(80, 100), 50,
                MaximumSparkPolicy.PRESERVE_PERCENTAGE).spark();

        assertEquals(new PlayerSpark(100, 100), restored);
        assertEquals(new PlayerSpark(50, 50), clamped);
        assertEquals(new PlayerSpark(40, 50), percent);
    }
}
