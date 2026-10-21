package io.github.kxyr0.echoanimation.forge.client.humanoid.state;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigKind;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationHumanoidRenderContext;
import io.github.kxyr0.echoanimation.forge.client.sync.AnimationClientSyncState;
import io.github.kxyr0.echoanimation.forge.debug.SkinningDebugState;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogState;
import java.util.Optional;

/** Resolves the segmented/debug rig used for a humanoid render pass. */
public final class HumanoidRigResolver {
    private static final AnimationRigId FALLBACK_SEGMENTED_RIG =
            AnimationRigId.parse("echo_animation:segmented_humanoid");
    private static final AnimationRigId DEBUG_RIG =
            AnimationRigId.parse("echo_animation:tests/full_humanoid");
    private static final AnimationRigId DEBUG_THERIG_RIG =
            AnimationRigId.parse("jdh:therig_2_6");

    private final AnimationCatalogState catalogState;

    public HumanoidRigResolver(AnimationCatalogState catalogState) {
        this.catalogState = java.util.Objects.requireNonNull(catalogState, "catalogState");
    }

    public Optional<AnimationRigDefinition> segmented(AnimationHumanoidRenderContext context) {
        for (AnimationRigId candidate : context.rigCandidates()) {
            Optional<AnimationRigDefinition> rig = catalogState.current().animationRigs().stream()
                    .filter(definition -> definition.id().equals(candidate))
                    .filter(definition -> definition.kind() == AnimationRigKind.SEGMENTED_HUMANOID)
                    .findFirst();
            if (rig.isPresent()) {
                return rig;
            }
        }
        Optional<AnimationRigDefinition> activeClipRig = activeClipRig(context);
        if (activeClipRig.isPresent()) {
            return activeClipRig;
        }
        return catalogState.current().animationRigs().stream()
                .filter(definition -> definition.id().equals(FALLBACK_SEGMENTED_RIG))
                .filter(definition -> definition.kind() == AnimationRigKind.SEGMENTED_HUMANOID)
                .findFirst();
    }

    public Optional<AnimationRigDefinition> debug() {
        AnimationRigId debugRig = debugRigId();
        return catalogState.current().animationRigs().stream()
                .filter(definition -> definition.id().equals(debugRig))
                .findFirst();
    }

    public Optional<AnimationRigDefinition> smoothDebug() {
        AnimationRigId renderRig = debugRigId();
        return catalogState.current().animationRigs().stream()
                .filter(definition -> definition.id().equals(renderRig))
                .filter(definition -> definition.kind() == AnimationRigKind.SKINNED_MESH)
                .findFirst();
    }

    public AnimationRigId debugRigId() {
        return SkinningDebugState.mode() == SkinningDebugState.Mode.THERIG_BIND
                ? DEBUG_THERIG_RIG
                : DEBUG_RIG;
    }

    private Optional<AnimationRigDefinition> activeClipRig(AnimationHumanoidRenderContext context) {
        var snapshot = AnimationClientSyncState.current(context.subjectId());
        if (snapshot.isEmpty()) {
            return Optional.empty();
        }
        for (var channel : snapshot.orElseThrow().channels()) {
            Optional<AnimationRigDefinition> current = channel.clipId().flatMap(this::rigForClip);
            if (current.isPresent()) {
                return current;
            }
            Optional<AnimationRigDefinition> previous = channel.previousClipId().flatMap(this::rigForClip);
            if (previous.isPresent()) {
                return previous;
            }
        }
        return Optional.empty();
    }

    private Optional<AnimationRigDefinition> rigForClip(AnimationClipId clipId) {
        Optional<AnimationRigId> rigId = catalogState.current().animationClips().stream()
                .filter(clip -> clip.id().equals(clipId))
                .flatMap(clip -> clip.rigId().stream())
                .findFirst();
        return rigId.flatMap(id -> catalogState.current().animationRigs().stream()
                .filter(rig -> rig.id().equals(id))
                .filter(rig -> rig.kind() == AnimationRigKind.SEGMENTED_HUMANOID)
                .findFirst());
    }
}
