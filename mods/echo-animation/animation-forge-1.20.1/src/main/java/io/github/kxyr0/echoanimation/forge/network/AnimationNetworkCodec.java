package io.github.kxyr0.echoanimation.forge.network;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.api.sync.AnimationChannelSyncState;
import io.github.kxyr0.echoanimation.api.sync.AnimationPlaybackSyncState;
import io.github.kxyr0.echoanimation.api.sync.AnimationSubjectSyncSnapshot;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncRevision;
import io.github.kxyr0.echoanimation.forge.network.codec.AnimationChannelNetworkCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;

final class AnimationNetworkCodec {
    private static final int MAX_ID = 256;
    private static final int MAX_CHANNELS = 32;
    private static final int MAX_PLAYBACKS = 32;

    private AnimationNetworkCodec() {
    }

    static void writeSnapshot(FriendlyByteBuf buffer, AnimationSubjectSyncSnapshot snapshot) {
        buffer.writeUtf(snapshot.subjectId().value(), MAX_ID);
        buffer.writeVarLong(snapshot.graphRevision());
        buffer.writeVarLong(snapshot.authoritativeTick());
        buffer.writeVarLong(snapshot.syncRevision().value());
        buffer.writeVarInt(snapshot.channels().size());
        snapshot.channels().forEach(channel -> AnimationChannelNetworkCodec.write(buffer, channel));
        buffer.writeVarInt(snapshot.playbacks().size());
        snapshot.playbacks().forEach(playback -> writePlayback(buffer, playback));
    }

    static AnimationSubjectSyncSnapshot readSnapshot(FriendlyByteBuf buffer) {
        AnimationSubjectId subjectId = new AnimationSubjectId(buffer.readUtf(MAX_ID));
        long graphRevision = buffer.readVarLong();
        long authoritativeTick = buffer.readVarLong();
        AnimationSyncRevision syncRevision = new AnimationSyncRevision(buffer.readVarLong());
        int channelCount = buffer.readVarInt();
        if (channelCount < 0 || channelCount > MAX_CHANNELS) {
            throw new IllegalArgumentException("Animation sync channel count is out of range: " + channelCount);
        }
        List<AnimationChannelSyncState> channels = new ArrayList<>();
        for (int index = 0; index < channelCount; index++) {
            channels.add(AnimationChannelNetworkCodec.read(buffer));
        }
        int playbackCount = buffer.readVarInt();
        if (playbackCount < 0 || playbackCount > MAX_PLAYBACKS) {
            throw new IllegalArgumentException("Animation sync playback count is out of range: " + playbackCount);
        }
        List<AnimationPlaybackSyncState> playbacks = new ArrayList<>();
        for (int index = 0; index < playbackCount; index++) {
            playbacks.add(readPlayback(buffer));
        }
        return new AnimationSubjectSyncSnapshot(subjectId, graphRevision, authoritativeTick, syncRevision, channels,
                playbacks);
    }

    static void writeRevision(FriendlyByteBuf buffer, AnimationSyncRevision revision) {
        buffer.writeVarLong(revision.value());
    }

    static AnimationSyncRevision readRevision(FriendlyByteBuf buffer) {
        return new AnimationSyncRevision(buffer.readVarLong());
    }

    static void writeSubjectId(FriendlyByteBuf buffer, AnimationSubjectId subjectId) {
        buffer.writeUtf(subjectId.value(), MAX_ID);
    }

    static AnimationSubjectId readSubjectId(FriendlyByteBuf buffer) {
        return new AnimationSubjectId(buffer.readUtf(MAX_ID));
    }


    private static void writePlayback(FriendlyByteBuf buffer, AnimationPlaybackSyncState playback) {
        buffer.writeUtf(playback.playbackId().value(), MAX_ID);
        buffer.writeUtf(playback.animationId().value(), MAX_ID);
        buffer.writeUtf(playback.definitionFingerprint(), MAX_ID);
        buffer.writeEnum(playback.phase());
        buffer.writeVarLong(playback.phaseEnteredTick());
        buffer.writeVarLong(playback.phaseElapsedTicks());
        buffer.writeVarLong(playback.mainLoopIteration());
        buffer.writeBoolean(playback.exitRequested());
        buffer.writeBoolean(playback.selectedExitPoint().isPresent());
        playback.selectedExitPoint().ifPresent(point -> buffer.writeUtf(point, MAX_ID));
        buffer.writeUtf(playback.targetState().value(), MAX_ID);
        buffer.writeVarLong(playback.playbackRevision());
    }

    private static AnimationPlaybackSyncState readPlayback(FriendlyByteBuf buffer) {
        AnimationPlaybackId playbackId = AnimationPlaybackId.parse(buffer.readUtf(MAX_ID));
        AnimationId animationId = AnimationId.parse(buffer.readUtf(MAX_ID));
        String fingerprint = buffer.readUtf(MAX_ID);
        AnimationPhase phase = buffer.readEnum(AnimationPhase.class);
        long enteredTick = buffer.readVarLong();
        long elapsedTicks = buffer.readVarLong();
        long loopIteration = buffer.readVarLong();
        boolean exitRequested = buffer.readBoolean();
        Optional<String> exitPoint = buffer.readBoolean() ? Optional.of(buffer.readUtf(MAX_ID)) : Optional.empty();
        AnimationStateId targetState = new AnimationStateId(buffer.readUtf(MAX_ID));
        long playbackRevision = buffer.readVarLong();
        return new AnimationPlaybackSyncState(playbackId, animationId, fingerprint, phase, enteredTick, elapsedTicks,
                loopIteration, exitRequested, exitPoint, targetState, playbackRevision);
    }

}
