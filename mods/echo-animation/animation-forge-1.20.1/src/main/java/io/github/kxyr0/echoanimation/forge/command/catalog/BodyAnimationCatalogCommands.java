package io.github.kxyr0.echoanimation.forge.command.catalog;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationBoneTrackDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogState;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

public final class BodyAnimationCatalogCommands {
    private final AnimationCatalogState catalogState;

    public BodyAnimationCatalogCommands(AnimationCatalogState catalogState) {
        this.catalogState = catalogState;
    }

    public int listBodyRigs(CommandSourceStack source) {
        List<String> ids = catalogState.current().animationRigs().stream() .map(rig -> rig.id().toString()) .sorted()
                        .toList();
        CatalogCommandOutput.sendList(source, "Echo Animation body_rigs", ids);
        return ids.size();
    }

    public int describeBodyRig(CommandSourceStack source, String rawId) {
        AnimationRigId id;
        try {
            id = AnimationRigId.parse(rawId);
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.literal(exception.getMessage()));
            return 0;
        }
        Optional<AnimationRigDefinition> rig = catalogState.current().animationRigs().stream()
                        .filter(candidate -> candidate.id().equals(id)) .findFirst();
        if (rig.isEmpty()) {
            source.sendFailure(Component.literal("Body rig not found: " + id));
            return 0;
        }
        AnimationRigDefinition definition = rig.orElseThrow();
        String bones = definition.bones().stream() .map(bone -> bone.id().value())
                        .reduce((left, right) -> left + "," + right) .orElse("none");
        source.sendSuccess( () ->
                        Component.literal( "Echo Animation body_rig " + id + ": kind=" + definition.kind() + ", skin="
                                        + definition.skinVariant() + ", bones=" + definition.bones().size() + " ["
                                        + bones + "]"), false);
        return 1;
    }

    public int describeBodyClip(CommandSourceStack source, String rawId) {
        AnimationClipId id;
        try {
            id = AnimationClipId.parse(rawId);
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.literal(exception.getMessage()));
            return 0;
        }
        Optional<AnimationClipDefinition> clip = catalogState.current().animationClips().stream()
                        .filter(candidate -> candidate.id().equals(id)) .findFirst();
        if (clip.isEmpty()) {
            source.sendFailure(Component.literal("Body clip not found: " + id));
            return 0;
        }
        AnimationClipDefinition definition = clip.orElseThrow();
        int keys = definition.tracks().stream().mapToInt(track -> track.keyframes().size()).sum();
        List<String> rotating = definition.tracks().stream() .filter(this::hasNonZeroRotation)
                        .map(track -> track.boneId().value()) .toList();
        List<String> translating = definition.tracks().stream() .filter(this::hasNonZeroTranslation)
                        .map(track -> track.boneId().value()) .toList();
        List<String> scaling = definition.tracks().stream() .filter(this::hasNonIdentityScale)
                        .map(track -> track.boneId().value()) .toList();
        long matrixTracks = definition.tracks().stream() .filter(AnimationBoneTrackDefinition::matrixTrack) .count();
        source.sendSuccess( () ->
                        Component.literal( "Echo Animation body_clip " + id + ": rig="
                                        + definition.rigId().map(Object::toString).orElse("none") + ", duration="
                                        + definition.durationTicks() + ", tracks=" + definition.tracks().size()
                                        + ", keys=" + keys + ", matrixTracks=" + matrixTracks + ", rotating=" + rotating
                                        + ", translating=" + translating + ", scaling=" + scaling), false);
        return 1;
    }

    public boolean hasNonZeroRotation(AnimationBoneTrackDefinition track) {
        return track.keyframes().stream() .flatMap(frame -> frame.rotationDegrees().stream()) .anyMatch(this::nonZero);
    }

    public boolean hasNonZeroTranslation(AnimationBoneTrackDefinition track) {
        return track.keyframes().stream() .flatMap(frame -> frame.translation().stream()) .anyMatch(this::nonZero);
    }

    public boolean hasNonIdentityScale(AnimationBoneTrackDefinition track) {
        return track.keyframes().stream() .flatMap(frame -> frame.scale().stream()) .anyMatch( value ->
                                Math.abs(value.x() - 1.0D) > 1.0E-6D || Math.abs(value.y() - 1.0D) > 1.0E-6D
                                        || Math.abs(value.z() - 1.0D) > 1.0E-6D);
    }

    public boolean nonZero(Vec3 value) {
        return Math.abs(value.x()) > 1.0E-6D || Math.abs(value.y()) > 1.0E-6D || Math.abs(value.z()) > 1.0E-6D;
    }

    public int listBodyClips(CommandSourceStack source) {
        List<String> ids = catalogState.current().animationClips().stream()
                        .map(clip -> clip.id() + clip.rigId().map(id -> " rig=" + id).orElse("")) .sorted() .toList();
        CatalogCommandOutput.sendList(source, "Echo Animation body_clips", ids);
        return ids.size();
    }

    public int listHighLevelAnimations(CommandSourceStack source) {
        List<String> ids = catalogState.current().animations().stream() .map( animation ->
                                        animation.id() + " channels=" + animation.controlledChannels()) .sorted()
                        .toList();
        CatalogCommandOutput.sendList(source, "Echo Animation animations", ids);
        return ids.size();
    }

    public int listProfiles(CommandSourceStack source) {
        List<String> ids = catalogState.current().animationProfiles().stream() .map( profile ->
                                        profile.id() + " rig=" + profile.rigId() + " graph=" + profile.graphId())
                        .sorted() .toList();
        CatalogCommandOutput.sendList(source, "Echo Animation profiles", ids);
        return ids.size();
    }
}
