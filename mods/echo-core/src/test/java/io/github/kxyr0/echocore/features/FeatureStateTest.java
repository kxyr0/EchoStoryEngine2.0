package io.github.kxyr0.echocore.features;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeatureStateTest {
    @Test void defaultsAreLockedAndFlagsAreIndependentForPlayersAndFeatures() {
        var state = new FeatureState(); var first = UUID.randomUUID(); var second = UUID.randomUUID();
        assertFalse(state.allowed(first, "thought_space_unlocked"));
        state.set(first, "thought_space_unlocked", true);
        assertTrue(state.allowed(first, "thought_space_unlocked"));
        assertFalse(state.allowed(first, "magic_unlocked"));
        assertFalse(state.allowed(second, "thought_space_unlocked"));
        state.set(first, "thought_space_unlocked", false);
        assertFalse(state.allowed(first, "thought_space_unlocked"));
        assertThrows(IllegalArgumentException.class, () -> state.set(first, "../bad", true));
    }
    @Test void savedFlagsRoundTripAndFreshWorldHasNoUnlocks() {
        var data = new FeatureSavedData(); var player = UUID.randomUUID();
        data.state().set(player, "magic_unlocked", true);
        var restored = FeatureSavedData.load(data.save(new net.minecraft.nbt.CompoundTag()));
        assertTrue(restored.state().allowed(player, "magic_unlocked"));
        assertFalse(new FeatureSavedData().state().allowed(player, "magic_unlocked"));
    }
    @Test void clientSnapshotsReplaceOldWorldAndCannotLeakToAnotherPlayer() {
        var player = UUID.randomUUID();
        FeatureClientState.receive(player, Map.of("magic_unlocked", true));
        assertTrue(FeatureClientState.allowed(player, "magic_unlocked"));
        assertFalse(FeatureClientState.allowed(UUID.randomUUID(), "magic_unlocked"));
        FeatureClientState.receive(player, Map.of());
        assertFalse(FeatureClientState.allowed(player, "magic_unlocked"));
        FeatureClientState.clear();
        assertFalse(FeatureClientState.allowed(player, "magic_unlocked"));
    }
}
