package io.github.kxyr0.echoanimation.forge.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.github.kxyr0.echoanimation.forge.debug.FaceDebugState;
import io.github.kxyr0.echoanimation.forge.debug.SkinningDebugState;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;

/** Manual visual diagnostics for authored face rigs. */
final class EchoAnimationDebugCommands {
    LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("debug").then(face()).then(skinning());
    }
    private LiteralArgumentBuilder<CommandSourceStack> face() {
        return Commands.literal("face_points")
                .then(mode("off", FaceDebugState.Mode.OFF))
                .then(mode("origins", FaceDebugState.Mode.ORIGINS))
                .then(mode("pivots", FaceDebugState.Mode.PIVOTS))
                .then(mode("all", FaceDebugState.Mode.ALL))
                .then(mode("on", FaceDebugState.Mode.ALL));
    }
    private LiteralArgumentBuilder<CommandSourceStack> skinning() {
        return Commands.literal("skinning")
                .then(Commands.literal("off").executes(c -> clearSkinning(c.getSource())))
                .then(npc(c -> targetNpc(c.getSource(), subject(c), SkinningDebugState.Mode.LEGACY)))
                .then(Commands.literal("matrix")
                        .then(npc(c -> targetNpc(c.getSource(), subject(c), SkinningDebugState.Mode.MATRIX))))
                .then(therig());
    }
    private LiteralArgumentBuilder<CommandSourceStack> therig() {
        return Commands.literal("therig")
                .then(Commands.literal("attachments")
                        .then(npc(c -> targetTheRigAttachments(c.getSource(), subject(c)))))
                .then(npc(c -> targetTheRig(c.getSource(), subject(c))))
                .then(Commands.literal("offset")
                        .then(Commands.argument("pixels", IntegerArgumentType.integer(-64, 64))
                                .then(npc(c -> targetTheRigWithOffset(c.getSource(), subject(c),
                                        IntegerArgumentType.getInteger(c, "pixels"))))));
    }
    private LiteralArgumentBuilder<CommandSourceStack> npc(
            java.util.function.ToIntFunction<com.mojang.brigadier.context.CommandContext<CommandSourceStack>> action) {
        return Commands.literal("npc").then(Commands.argument("subject_id", ResourceLocationArgument.id())
                .executes(c -> action.applyAsInt(c)));
    }
    private String subject(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        return ResourceLocationArgument.getId(context, "subject_id").toString();
    }

    private LiteralArgumentBuilder<CommandSourceStack> mode(String name, FaceDebugState.Mode mode) {
        return Commands.literal(name).executes(context -> setMode(context.getSource(), mode));
    }

    private int setMode(CommandSourceStack source, FaceDebugState.Mode mode) {
        FaceDebugState.set(mode);
        source.sendSuccess( () ->
                        Component.literal( "Echo Animation face debug points: " + mode.name().toLowerCase()), false);
        return 1;
    }

    private int targetNpc( CommandSourceStack source, String subjectId, SkinningDebugState.Mode mode) {
        SkinningDebugState.clearTherigOffsetY();
        SkinningDebugState.therigAttachmentsEnabled(false);
        SkinningDebugState.target(subjectId, mode);
        source.sendSuccess( () ->
                        Component.literal( "Smooth-skinning NPC [" + mode.name().toLowerCase() + "]: " + subjectId),
                false);
        return 1;
    }

    private int targetTheRig(CommandSourceStack source, String subjectId) {
        SkinningDebugState.clearTherigOffsetY();
        SkinningDebugState.therigAttachmentsEnabled(false);
        return targetNpc(source, subjectId, SkinningDebugState.Mode.THERIG_BIND);
    }

    private int targetTheRigAttachments(CommandSourceStack source, String subjectId) {
        SkinningDebugState.clearTherigOffsetY();
        SkinningDebugState.target(subjectId, SkinningDebugState.Mode.THERIG_BIND);
        SkinningDebugState.therigAttachmentsEnabled(true);
        source.sendSuccess( () -> Component.literal("Smooth-skinning NPC [therig_attachments]: " + subjectId), false);
        return 1;
    }

    private int targetTheRigWithOffset(CommandSourceStack source, String subjectId, int pixels) {
        SkinningDebugState.target(subjectId, SkinningDebugState.Mode.THERIG_BIND);
        SkinningDebugState.therigAttachmentsEnabled(false);
        SkinningDebugState.therigOffsetY(pixels);
        source.sendSuccess( () ->
                        Component.literal( "Smooth-skinning NPC [therig_bind] Y offset=" + pixels + "px: " + subjectId),
                false);
        return 1;
    }

    private int clearSkinning(CommandSourceStack source) {
        SkinningDebugState.clear();
        source.sendSuccess(() -> Component.literal("Smooth-skinning NPC: off"), false);
        return 1;
    }
}
