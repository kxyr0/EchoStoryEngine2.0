package io.github.kxyr0.echoanimation.forge.playback.sync;

import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncRevision;
import io.github.kxyr0.echoanimation.api.sync.AnimationSubjectSyncSnapshot;
import io.github.kxyr0.echoanimation.definition.resource.AnimationResourceSet;
import io.github.kxyr0.echoanimation.forge.network.AnimationNetworkChannel;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeSavedData;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.PlaybackRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.SubjectRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationSubjectSnapshotWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Converts durable subject records into client sync packets.
 */
public final class AnimationSubjectSnapshotPublisher {
    private final AnimationSubjectSnapshotWriter snapshots = new AnimationSubjectSnapshotWriter();

    public void sendFullSnapshots(
            AnimationRuntimeSavedData data,
            AnimationResourceSet resources,
            MinecraftServer server,
            ServerPlayer player
    ) {
        data.subjects.values().forEach(subject -> AnimationNetworkChannel.sendFullSnapshot(player,
                snapshot(data, resources, server, subject)));
    }

    public void publish(
            AnimationRuntimeSavedData data,
            AnimationResourceSet resources,
            MinecraftServer server,
            AnimationSubjectId subjectId,
            boolean full
    ) {
        SubjectRecord subject = data.subjects.get(subjectId);
        if (subject == null || server == null) {
            return;
        }
        AnimationSubjectSyncSnapshot snapshot = snapshot(data, resources, server, subject);
        server.getPlayerList().getPlayers().forEach(player -> {
            if (full) {
                AnimationNetworkChannel.sendFullSnapshot(player, snapshot);
            } else {
                AnimationNetworkChannel.sendSnapshot(player, snapshot);
            }
        });
    }

    public void remove(MinecraftServer server, AnimationSubjectId subjectId, AnimationSyncRevision revision) {
        if (server != null) {
            server.getPlayerList().getPlayers().forEach(player ->
                    AnimationNetworkChannel.sendSubjectRemoval(player, subjectId, revision));
        }
    }

    private AnimationSubjectSyncSnapshot snapshot(
            AnimationRuntimeSavedData data,
            AnimationResourceSet resources,
            MinecraftServer server,
            SubjectRecord subject
    ) {
        List<PlaybackRecord> active = data.activePlaybacks.values().stream()
                .filter(playback -> playback.subjectId.equals(subject.subjectId)).toList();
        Map<String, AnimationDefinition> definitions = new LinkedHashMap<>();
        data.definitionArchive.forEach((fingerprint, archive) -> definitions.put(fingerprint, archive.definition()));
        return snapshots.snapshot(new AnimationSubjectSnapshotWriter.SnapshotInput(subject, active, definitions,
                clips(resources), data.repositoryRevision, serverTick(server)));
    }

    private Map<AnimationClipId, AnimationClipDefinition> clips(AnimationResourceSet resources) {
        return resources.animationClips().stream().collect(Collectors.toMap(AnimationClipDefinition::id,
                clip -> clip, (left, right) -> left, LinkedHashMap::new));
    }

    private long serverTick(MinecraftServer server) {
        return server == null ? 0L : server.overworld().getGameTime();
    }
}
