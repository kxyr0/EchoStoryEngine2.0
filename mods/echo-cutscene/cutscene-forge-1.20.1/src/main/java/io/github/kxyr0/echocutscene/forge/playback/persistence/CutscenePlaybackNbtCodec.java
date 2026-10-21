package io.github.kxyr0.echocutscene.forge.playback.persistence;

import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutscenePlaybackId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneEndBehavior;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneStatus;
import io.github.kxyr0.echocutscene.forge.body.PlayerBodySnapshot;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

final class CutscenePlaybackNbtCodec {
    CompoundTag write(Map<UUID, SavedCutscenePlayback> playbacks, CompoundTag tag) {
        ListTag list = new ListTag();
        playbacks.values().forEach(playback -> list.add(writePlayback(playback)));
        tag.put("playbacks", list);
        return tag;
    }

    void read(CompoundTag tag, Map<UUID, SavedCutscenePlayback> output) {
        ListTag list = tag.getList("playbacks", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            readPlayback(list.getCompound(index)).ifPresent(playback -> output.put(playback.playerId(), playback));
        }
    }

    private CompoundTag writePlayback(SavedCutscenePlayback playback) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("playerId", playback.playerId());
        tag.putString("playbackId", playback.playbackId().value());
        tag.putString("cutsceneId", playback.cutsceneId().value());
        tag.putString("endBehavior", playback.endBehavior().name());
        tag.putString("status", playback.status().name());
        tag.putLong("elapsedMillis", playback.elapsedMillis());
        tag.put("body", writeBody(playback.bodySnapshot()));
        return tag;
    }

    private Optional<SavedCutscenePlayback> readPlayback(CompoundTag tag) {
        try {
            UUID playerId = tag.getUUID("playerId");
            PlayerBodySnapshot body = readBody(playerId, tag.getCompound("body"));
            return Optional.of(new SavedCutscenePlayback(playerId,
                    new CutscenePlaybackId(tag.getString("playbackId")),
                    CutsceneId.parse(tag.getString("cutsceneId")),
                    CutsceneEndBehavior.valueOf(tag.getString("endBehavior")),
                    CutsceneStatus.valueOf(tag.getString("status")),
                    tag.getLong("elapsedMillis"), body));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private CompoundTag writeBody(PlayerBodySnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("x", snapshot.x());
        tag.putDouble("y", snapshot.y());
        tag.putDouble("z", snapshot.z());
        tag.putFloat("yaw", snapshot.yaw());
        tag.putFloat("pitch", snapshot.pitch());
        tag.putString("pose", snapshot.pose().name());
        snapshot.vehicleId().ifPresent(vehicleId -> tag.putUUID("vehicleId", vehicleId));
        tag.putInt("selectedSlot", snapshot.selectedSlot());
        tag.put("mainHand", snapshot.mainHand().save(new CompoundTag()));
        tag.put("offHand", snapshot.offHand().save(new CompoundTag()));
        tag.putInt("gameType", snapshot.gameType().getId());
        return tag;
    }

    private PlayerBodySnapshot readBody(UUID playerId, CompoundTag tag) {
        return new PlayerBodySnapshot(playerId, tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"),
                tag.getFloat("yaw"), tag.getFloat("pitch"), pose(tag.getString("pose")), vehicle(tag),
                tag.getInt("selectedSlot"), ItemStack.of(tag.getCompound("mainHand")),
                ItemStack.of(tag.getCompound("offHand")), GameType.byId(tag.getInt("gameType")));
    }

    private static Pose pose(String value) {
        try {
            return Pose.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return Pose.STANDING;
        }
    }

    private static Optional<UUID> vehicle(CompoundTag tag) {
        return tag.hasUUID("vehicleId") ? Optional.of(tag.getUUID("vehicleId")) : Optional.empty();
    }
}
