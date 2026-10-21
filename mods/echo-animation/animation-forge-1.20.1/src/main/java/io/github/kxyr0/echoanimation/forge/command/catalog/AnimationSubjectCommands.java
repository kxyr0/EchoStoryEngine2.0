package io.github.kxyr0.echoanimation.forge.command.catalog;

import io.github.kxyr0.echoanimation.api.profile.definition.AnimationProfileDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.PlaybackRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeSavedData;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogState;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

public final class AnimationSubjectCommands {
    private final AnimationCatalogState catalogState;

    public AnimationSubjectCommands(AnimationCatalogState catalogState) {
        this.catalogState = catalogState;
    }

    public int describeSubject(CommandSourceStack source, String rawSubjectId) {
        AnimationSubjectId subjectId = new AnimationSubjectId(rawSubjectId);
        AnimationRuntimeSavedData data = AnimationRuntimeSavedData.load(source.getServer());
        var subject = data.subjects.get(subjectId);
        if (subject == null) {
            source.sendFailure(Component.literal("Animation subject not found: " + subjectId));
            return 0;
        }
        Optional<AnimationProfileDefinition> profile = subject.profileId.flatMap( profileId ->
                                catalogState.current().animationProfiles().stream()
                                        .filter(candidate -> candidate.id().equals(profileId)) .findFirst());
        Optional<AnimationRigDefinition> rig = profile.flatMap( definition ->
                                catalogState.current().animationRigs().stream() .filter( candidate ->
                                                        candidate.id().equals(definition.rigId())) .findFirst());
        String profileText = subject.profileId.map(Object::toString).orElse("none");
        String rigText = profile.map(definition -> definition.rigId().toString()).orElse("none");
        String renderText = profile.map(definition -> definition.renderModelKind().name()).orElse("none");
        String rigDetails = rig.map( definition ->
                                        definition.kind() + "/" + definition.bones().size() + " bones")
                        .orElse("missing");
        String channels = subject.channelStates.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .reduce((left, right) -> left + "," + right) .orElse("none");
        List<PlaybackRecord> playbacks = data.activePlaybacks.values().stream()
                        .filter(playback -> playback.subjectId.equals(subjectId)) .toList();
        String playbackText = playbacks.stream() .map( playback ->
                                        playback.animationId + " phase=" + playback.phase + " clip="
                                                + activeClip(playback) + " elapsed=" + playback.phaseElapsedTicks
                                                + " speed=" + playback.playbackSpeed)
                        .reduce((left, right) -> left + " | " + right) .orElse("none");
        source.sendSuccess( () ->
                        Component.literal( "Echo Animation subject " + subjectId + ": profile=" + profileText + ", rig="
                                        + rigText + " (" + rigDetails + ")" + ", render=" + renderText + ", channels=["
                                        + channels + "]" + ", active=[" + playbackText + "]"), false);
        return 1;
    }

    public String activeClip(PlaybackRecord playback) {
        return switch (playback.phase) {
            case ENTER -> playback.entryClipId.toString();
            case MAIN -> playback.mainClipId.toString();
            case EXIT -> playback.exitClipId.map(Object::toString).orElse("none");
            case TERMINAL -> "none";
        };
    }
}
