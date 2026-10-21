package io.github.kxyr0.echoplayer.api.spark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class PlayerSparkTest {
    @Test
    void validSparkSnapshotRoundTrips() {
        PlayerSpark spark = new PlayerSpark(70, 100);

        assertEquals(spark, PlayerSparkSnapshot.from(spark).toSpark());
    }

    @Test
    void invalidCurrentOrMaximumFails() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerSpark(-1, 100));
        assertThrows(IllegalArgumentException.class, () -> new PlayerSpark(101, 100));
        assertThrows(IllegalArgumentException.class, () -> new PlayerSpark(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PlayerSpark(0, 1_000_001));
    }
}
