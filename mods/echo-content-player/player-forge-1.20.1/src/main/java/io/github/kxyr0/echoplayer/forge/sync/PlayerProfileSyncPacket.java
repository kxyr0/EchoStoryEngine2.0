package io.github.kxyr0.echoplayer.forge.sync;

import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinMode;
import io.github.kxyr0.echoplayer.api.identity.PlayerNameMode;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Compact immutable profile snapshot packet.
 */
public record PlayerProfileSyncPacket(
        UUID accountUuid,
        String accountName,
        String resolvedName,
        PlayerNameMode nameMode,
        int sparkCurrent,
        int sparkMaximum,
        PlayerSkinMode skinMode,
        String skinId,
        String skinModel,
        long profileRevision,
        long repositoryRevision,
        long appearanceRevision
) {
    public static PlayerProfileSyncPacket from(PlayerProfileSnapshot snapshot) {
        return new PlayerProfileSyncPacket(snapshot.playerId().accountUuid(), snapshot.identity().accountName(),
                snapshot.identity().resolvedDisplayName(), snapshot.identity().nameMode(),
                snapshot.spark().current(), snapshot.spark().maximum(), snapshot.appearance().skinMode(),
                snapshot.appearance().skinId().map(Object::toString).orElse(""),
                snapshot.appearance().skinModel().map(Object::toString).orElse(""),
                snapshot.profileRevision().value(), snapshot.repositoryRevision(),
                snapshot.appearance().appearanceRevision());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(accountUuid);
        buffer.writeUtf(accountName);
        buffer.writeUtf(resolvedName);
        buffer.writeUtf(nameMode.name());
        buffer.writeInt(sparkCurrent);
        buffer.writeInt(sparkMaximum);
        buffer.writeUtf(skinMode.name());
        buffer.writeUtf(skinId);
        buffer.writeUtf(skinModel);
        buffer.writeLong(profileRevision);
        buffer.writeLong(repositoryRevision);
        buffer.writeLong(appearanceRevision);
    }

    public static PlayerProfileSyncPacket decode(FriendlyByteBuf buffer) {
        return new PlayerProfileSyncPacket(buffer.readUUID(), buffer.readUtf(), buffer.readUtf(),
                PlayerNameMode.valueOf(buffer.readUtf()), buffer.readInt(), buffer.readInt(),
                PlayerSkinMode.valueOf(buffer.readUtf()), buffer.readUtf(), buffer.readUtf(),
                buffer.readLong(), buffer.readLong(), buffer.readLong());
    }
}
