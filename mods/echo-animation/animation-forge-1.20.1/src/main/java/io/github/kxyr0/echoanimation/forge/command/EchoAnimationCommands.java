package io.github.kxyr0.echoanimation.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.forge.command.catalog.AnimationSubjectCommands;
import io.github.kxyr0.echoanimation.forge.command.catalog.BodyAnimationCatalogCommands;
import io.github.kxyr0.echoanimation.forge.command.catalog.FaceAnimationCatalogCommands;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogState;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;

/** Server commands for inspecting the installed animation catalog. */
public final class EchoAnimationCommands {
    private final AnimationCatalogState catalogState;
    private final FaceAnimationCatalogCommands face;
    private final BodyAnimationCatalogCommands body;
    private final AnimationSubjectCommands subjects;

    public EchoAnimationCommands(AnimationCatalogState catalogState) {
        this.catalogState = java.util.Objects.requireNonNull(catalogState, "catalogState");
        face = new FaceAnimationCatalogCommands(catalogState);
        body = new BodyAnimationCatalogCommands(catalogState);
        subjects = new AnimationSubjectCommands(catalogState);
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("echo_animation");
        root.then(plain("health", this::reportHealth));
        root.then(plain("list", this::listAll));
        root.then(plain("catalog", this::reportHealth));
        root.then(plain("face_rigs", face::listRigs));
        root.then(inspectCatalog("face_rig", face::describeRig));
        root.then(plain("face_poses", face::listPoses));
        root.then(plain("face_animations", face::listAnimations));
        root.then(plain("body_rigs", body::listBodyRigs));
        root.then(inspectCatalog("body_rig", body::describeBodyRig));
        root.then(plain("body_clips", body::listBodyClips));
        root.then(inspectCatalog("body_clip", body::describeBodyClip));
        root.then(plain("animations", body::listHighLevelAnimations));
        root.then(plain("profiles", body::listProfiles));
        root.then(Commands.literal("subject").then(Commands.argument("subject_id", ResourceLocationArgument.id())
                .executes(context -> subjects.describeSubject(context.getSource(),
                        ResourceLocationArgument.getId(context, "subject_id").toString()))));
        root.then(new EchoAnimationAcceptanceCommands(catalogState).node());
        root.then(new EchoAnimationDebugCommands().node());
        root.then(Commands.literal("diagnostics").executes(context -> listDiagnostics(context.getSource(), 20))
                .then(Commands.argument("limit", IntegerArgumentType.integer(1, 100))
                        .executes(context -> listDiagnostics(context.getSource(),
                                IntegerArgumentType.getInteger(context, "limit")))));
        dispatcher.register(root);
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> plain(
            String name, java.util.function.ToIntFunction<CommandSourceStack> action) {
        return Commands.literal(name).executes(context -> action.applyAsInt(context.getSource()));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> inspectCatalog(
            String name, java.util.function.BiFunction<CommandSourceStack, String, Integer> action) {
        return Commands.literal(name).then(Commands.argument("id", StringArgumentType.greedyString())
                .executes(context -> action.apply(context.getSource(), StringArgumentType.getString(context, "id"))));
    }

    private int reportHealth(CommandSourceStack source) {
        int faceRigs = catalogState.current().faceRigs().size();
        int facePoses = catalogState.current().facePoses().size();
        int faceAnimations = catalogState.current().facialAnimations().size();
        int bodyRigs = catalogState.current().animationRigs().size();
        int bodyClips = catalogState.current().animationClips().size();
        int animations = catalogState.current().animations().size();
        int profiles = catalogState.current().animationProfiles().size();
        int diagnostics = catalogState.diagnostics().size();
        source.sendSuccess(
                () ->
                        Component.literal(
                                "Echo Animation: face_rigs="
                                        + faceRigs
                                        + ", face_poses="
                                        + facePoses
                                        + ", face_animations="
                                        + faceAnimations
                                        + ", body_rigs="
                                        + bodyRigs
                                        + ", body_clips="
                                        + bodyClips
                                        + ", animations="
                                        + animations
                                        + ", animation_profiles="
                                        + profiles
                                        + ", diagnostics="
                                        + diagnostics),
                false);
        return faceRigs + facePoses + faceAnimations + bodyRigs + bodyClips + animations + profiles;
    }

    private int listAll(CommandSourceStack source) {
        return face.listRigs(source)
                + face.listPoses(source)
                + face.listAnimations(source)
                + body.listBodyRigs(source)
                + body.listBodyClips(source)
                + body.listHighLevelAnimations(source)
                + body.listProfiles(source);
    }

    private int listDiagnostics(CommandSourceStack source, int limit) {
        List<AnimationDiagnostic> diagnostics =
                catalogState.diagnostics().stream()
                        .sorted(
                                Comparator.comparing(AnimationDiagnostic::error)
                                        .reversed()
                                        .thenComparing(AnimationDiagnostic::code))
                        .limit(limit)
                        .toList();
        if (diagnostics.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Echo Animation diagnostics: none"), false);
            return 0;
        }
        diagnostics.forEach(
                diagnostic ->
                        source.sendSuccess(
                                () ->
                                        Component.literal(
                                                "Echo Animation diagnostic "
                                                        + diagnostic.severity()
                                                        + " ["
                                                        + diagnostic.code()
                                                        + "] "
                                                        + diagnostic.message()),
                                false));
        return diagnostics.size();
    }
}
