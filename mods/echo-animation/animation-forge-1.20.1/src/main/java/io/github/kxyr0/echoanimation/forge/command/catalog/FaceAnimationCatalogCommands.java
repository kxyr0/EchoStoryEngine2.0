package io.github.kxyr0.echoanimation.forge.command.catalog;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationBoneTrackDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.profile.definition.AnimationProfileDefinition;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.PlaybackRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeSavedData;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogState;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;

public final class FaceAnimationCatalogCommands {
    private final AnimationCatalogState catalogState;
    public FaceAnimationCatalogCommands(AnimationCatalogState catalogState) { this.catalogState = catalogState; }
    public int listRigs(CommandSourceStack source) {
        List<String> ids = catalogState.current().faceRigs().stream()
                .map(rig -> rig.id().toString())
                .sorted()
                .toList();
        CatalogCommandOutput.sendList(source, "Echo Animation face_rigs", ids);
        return ids.size();
    }

    public int describeRig(CommandSourceStack source, String rawId) {
        FaceRigId id;
        try {
            id = FaceRigId.parse(rawId);
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.literal(exception.getMessage()));
            return 0;
        }
        Optional<FaceRigDefinition> rig = catalogState.current().faceRigs().stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst();
        if (rig.isEmpty()) {
            source.sendFailure(Component.literal("FaceRig не найден: " + id));
            return 0;
        }
        FaceRigDefinition definition = rig.orElseThrow();
        source.sendSuccess(() -> Component.literal("FaceRig " + id
                + ": bones=" + definition.hierarchy().bones().size()
                + ", layers=" + definition.layers().size()
                + ", controls=" + definition.controls().size()), false);
        return 1;
    }

    public int listPoses(CommandSourceStack source) {
        List<String> ids = catalogState.current().facePoses().stream()
                .map(pose -> pose.id() + " rig=" + pose.rigId())
                .sorted()
                .toList();
        CatalogCommandOutput.sendList(source, "Echo Animation face_poses", ids);
        return ids.size();
    }

    public int listAnimations(CommandSourceStack source) {
        List<String> ids = catalogState.current().facialAnimations().stream()
                .map(animation -> animation.id() + animation.rigId().map(id -> " rig=" + id).orElse(""))
                .sorted()
                .toList();
        CatalogCommandOutput.sendList(source, "Echo Animation face_animations", ids);
        return ids.size();
    }
}
