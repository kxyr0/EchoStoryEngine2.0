package io.github.kxyr0.echoplayervitals.forge.sync;

import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigue;
import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigueSnapshot;
import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigueStage;
import io.github.kxyr0.echoplayervitals.api.health.PlayerHealthSnapshot;
import io.github.kxyr0.echoplayervitals.api.hunger.PlayerHungerSnapshot;
import io.github.kxyr0.echoplayervitals.api.identity.PlayerVitalsId;
import io.github.kxyr0.echoplayervitals.api.revision.PlayerVitalsRevision;
import io.github.kxyr0.echoplayervitals.api.snapshot.PlayerVitalsSnapshot;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Compact immutable Vitals snapshot packet.
 */
public record PlayerVitalsSyncPacket(
        UUID accountUuid,
        int fatigueCurrent,
        int fatigueMaximum,
        PlayerFatigueStage stage,
        double movementMultiplier,
        double nauseaIntensity,
        float healthCurrent,
        float healthMaximum,
        boolean alive,
        long revision
) {
    public static PlayerVitalsSyncPacket from(PlayerVitalsSnapshot snapshot) {
        return new PlayerVitalsSyncPacket(snapshot.playerId().accountUuid(),
                snapshot.fatigue().fatigue().current(), snapshot.fatigue().fatigue().maximum(),
                snapshot.fatigue().stage(), snapshot.fatigue().movementMultiplier(),
                snapshot.fatigue().nauseaIntensity(), snapshot.health().current(), snapshot.health().maximum(),
                snapshot.health().alive(), snapshot.revision().value());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(accountUuid);
        buffer.writeInt(fatigueCurrent);
        buffer.writeInt(fatigueMaximum);
        buffer.writeUtf(stage.name());
        buffer.writeDouble(movementMultiplier);
        buffer.writeDouble(nauseaIntensity);
        buffer.writeFloat(healthCurrent);
        buffer.writeFloat(healthMaximum);
        buffer.writeBoolean(alive);
        buffer.writeLong(revision);
    }

    public static PlayerVitalsSyncPacket decode(FriendlyByteBuf buffer) {
        return new PlayerVitalsSyncPacket(buffer.readUUID(), buffer.readInt(), buffer.readInt(),
                PlayerFatigueStage.valueOf(buffer.readUtf()), buffer.readDouble(), buffer.readDouble(),
                buffer.readFloat(), buffer.readFloat(), buffer.readBoolean(), buffer.readLong());
    }

    public PlayerVitalsSnapshot toSnapshot() {
        PlayerVitalsRevision snapshotRevision = new PlayerVitalsRevision(revision);
        PlayerFatigue fatigue = new PlayerFatigue(fatigueCurrent, fatigueMaximum);
        PlayerFatigueSnapshot fatigueSnapshot = new PlayerFatigueSnapshot(fatigue, fatigue.reserve(), stage,
                movementMultiplier, nauseaIntensity, snapshotRevision);
        return new PlayerVitalsSnapshot(PlayerVitalsId.fromAccountUuid(accountUuid), fatigueSnapshot,
                new PlayerHealthSnapshot(healthCurrent, healthMaximum, alive), PlayerHungerSnapshot.neutral(),
                snapshotRevision);
    }
}
