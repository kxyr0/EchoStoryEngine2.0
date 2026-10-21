package io.github.kxyr0.echoanimation.forge.network.codec;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.graph.definition.ChannelCompositionPolicy;
import io.github.kxyr0.echoanimation.api.graph.definition.PlaybackMode;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.sync.AnimationChannelSyncState;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Wire codec for one channel cursor, including source clip segment references.
 */
public final class AnimationChannelNetworkCodec {
    private static final int MAX_ID = 256;
    private static final int MAX_BONES = 64;

    private AnimationChannelNetworkCodec() {
    }

    public static void write(FriendlyByteBuf buffer, AnimationChannelSyncState channel) {
        buffer.writeUtf(channel.channelId().value(), MAX_ID);
        buffer.writeUtf(channel.stateId().value(), MAX_ID);
        writeClipId(buffer, channel.clipId());
        writeString(buffer, channel.segmentId());
        writeString(buffer, channel.pointId());
        buffer.writeVarLong(channel.localTick());
        buffer.writeVarLong(channel.elapsedTicks());
        buffer.writeVarLong(channel.durationTicks());
        buffer.writeDouble(channel.normalizedProgress());
        buffer.writeVarLong(channel.loopIteration());
        buffer.writeBoolean(channel.clipComplete());
        buffer.writeBoolean(channel.waitingForExit());
        buffer.writeEnum(channel.playbackMode());
        buffer.writeDouble(channel.playbackSpeed());
        buffer.writeVarInt(channel.visualOrder());
        buffer.writeEnum(channel.compositionPolicy());
        writeBoneMask(buffer, channel.boneMask());
        buffer.writeDouble(channel.weight());
        writeClipId(buffer, channel.previousClipId());
        buffer.writeVarLong(channel.transitionElapsedTicks());
        buffer.writeVarLong(channel.transitionDurationTicks());
        buffer.writeDouble(channel.transitionProgress());
    }

    public static AnimationChannelSyncState read(FriendlyByteBuf buffer) {
        AnimationChannelId channelId = new AnimationChannelId(buffer.readUtf(MAX_ID));
        AnimationStateId stateId = new AnimationStateId(buffer.readUtf(MAX_ID));
        Optional<AnimationClipId> clipId = readClipId(buffer);
        Optional<String> segmentId = readString(buffer);
        Optional<String> pointId = readString(buffer);
        long localTick = buffer.readVarLong();
        long elapsedTicks = buffer.readVarLong();
        long durationTicks = buffer.readVarLong();
        double normalizedProgress = buffer.readDouble();
        long loopIteration = buffer.readVarLong();
        boolean clipComplete = buffer.readBoolean();
        boolean waitingForExit = buffer.readBoolean();
        PlaybackMode playbackMode = buffer.readEnum(PlaybackMode.class);
        double playbackSpeed = buffer.readDouble();
        int visualOrder = buffer.readVarInt();
        ChannelCompositionPolicy compositionPolicy = buffer.readEnum(ChannelCompositionPolicy.class);
        Set<BoneId> boneMask = readBoneMask(buffer);
        double weight = buffer.readDouble();
        Optional<AnimationClipId> previousClipId = readClipId(buffer);
        long transitionElapsedTicks = buffer.readVarLong();
        long transitionDurationTicks = buffer.readVarLong();
        double transitionProgress = buffer.readDouble();
        return new AnimationChannelSyncState(channelId, stateId, clipId, segmentId, pointId, localTick, elapsedTicks,
                durationTicks, normalizedProgress, loopIteration, clipComplete, waitingForExit, playbackMode,
                playbackSpeed, visualOrder, compositionPolicy, boneMask, weight, previousClipId,
                transitionElapsedTicks, transitionDurationTicks, transitionProgress);
    }

    private static void writeClipId(FriendlyByteBuf buffer, Optional<AnimationClipId> value) {
        buffer.writeBoolean(value.isPresent());
        value.ifPresent(id -> buffer.writeUtf(id.value(), MAX_ID));
    }

    private static Optional<AnimationClipId> readClipId(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? Optional.of(AnimationClipId.parse(buffer.readUtf(MAX_ID))) : Optional.empty();
    }

    private static void writeString(FriendlyByteBuf buffer, Optional<String> value) {
        buffer.writeBoolean(value.isPresent());
        value.ifPresent(text -> buffer.writeUtf(text, MAX_ID));
    }

    private static Optional<String> readString(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? Optional.of(buffer.readUtf(MAX_ID)) : Optional.empty();
    }

    private static void writeBoneMask(FriendlyByteBuf buffer, Set<BoneId> mask) {
        buffer.writeVarInt(mask.size());
        mask.stream().sorted(java.util.Comparator.comparing(BoneId::value))
                .forEach(bone -> buffer.writeUtf(bone.value(), MAX_ID));
    }

    private static Set<BoneId> readBoneMask(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_BONES) {
            throw new IllegalArgumentException("Animation sync bone-mask count is out of range: " + count);
        }
        Set<BoneId> result = new LinkedHashSet<>();
        for (int index = 0; index < count; index++) {
            result.add(BoneId.parse(buffer.readUtf(MAX_ID)));
        }
        return result;
    }
}
