package io.github.kxyr0.echoteleportmagic.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echoteleportmagic.api.TeleportDestination;
import io.github.kxyr0.echoteleportmagic.api.TeleportExecutionId;
import io.github.kxyr0.echoteleportmagic.forge.EchoTeleportMagic;
import io.github.kxyr0.echoteleportmagic.forge.network.TeleportNetwork;
import io.github.kxyr0.echoteleportmagic.forge.network.TeleportVisualPacket;
import io.github.kxyr0.echoteleportmagic.forge.server.TeleportSubjects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

public final class TeleportCommands {
    private TeleportCommands() { }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_teleport_magic").requires(s -> s.hasPermission(2))
                .then(Commands.literal("status").executes(c -> {
                    c.getSource().sendSuccess(() -> Component.literal("Active: " + EchoTeleportMagic.service().coordinator().active()), false); return 1;
                }))
                .then(Commands.literal("preview").then(Commands.argument("entity", EntityArgument.entity()).executes(c ->
                        start(c.getSource(), EntityArgument.getEntity(c, "entity"), null))))
                .then(Commands.literal("teleport").then(Commands.argument("entity", EntityArgument.entity())
                        .then(Commands.argument("destination", StringArgumentType.string()).executes(c ->
                                named(c.getSource(), EntityArgument.getEntity(c, "entity"), StringArgumentType.getString(c, "destination"))))))
                .then(Commands.literal("teleport_xyz").then(Commands.argument("entity", EntityArgument.entity())
                        .then(Commands.argument("position", Vec3Argument.vec3()).executes(c -> {
                            var p = Vec3Argument.getVec3(c, "position");
                            return start(c.getSource(), EntityArgument.getEntity(c, "entity"), new TeleportDestination("debug:position",
                                    c.getSource().getLevel().dimension().location().toString(), p.x, p.y, p.z, 0, 0));
                        }))))
                .then(Commands.literal("cancel").then(Commands.argument("execution", StringArgumentType.word()).executes(c -> {
                    EchoTeleportMagic.service().coordinator().cancel(new TeleportExecutionId(java.util.UUID.fromString(
                            StringArgumentType.getString(c, "execution")))); return 1;
                })))
                .then(Commands.literal("debug")
                        .then(Commands.literal("on").executes(c -> debug(c.getSource(), -1)))
                        .then(Commands.literal("off").executes(c -> debug(c.getSource(), -2)))
                        .then(Commands.literal("freeze").then(Commands.argument("seconds", DoubleArgumentType.doubleArg(0, 4))
                                .executes(c -> debug(c.getSource(), DoubleArgumentType.getDouble(c, "seconds")))))));
    }
    private static int start(CommandSourceStack source, net.minecraft.world.entity.Entity entity, TeleportDestination destination) {
        if (!(entity instanceof LivingEntity subject)) throw new IllegalArgumentException("LivingEntity required");
        if (destination == null) destination = new TeleportDestination("debug:preview", subject.level().dimension().location().toString(),
                subject.getX(), subject.getY(), subject.getZ(), subject.getYRot(), subject.getXRot());
        boolean preview = destination.locationId().equals("debug:preview");
        var result = EchoTeleportMagic.service().start(subject, destination, TeleportExecutionId.random(),
                "debug", "brigadier", preview, ignored -> { });
        source.sendSuccess(() -> Component.literal(result.id().value().toString()), false);
        return 1;
    }
    private static int named(CommandSourceStack source, net.minecraft.world.entity.Entity entity, String reference) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("echo_audio")) {
            throw new IllegalArgumentException("Named Location provider unavailable; use teleport_xyz for debugging");
        }
        return start(source, entity, io.github.kxyr0.echoteleportmagic.forge.integration.NamedTeleportDestination.resolve(reference));
    }
    private static int debug(CommandSourceStack source, double freeze) {
        var subject = source.getEntity();
        if (!(subject instanceof LivingEntity living) || !(subject instanceof net.minecraft.server.level.ServerPlayer player)) return 0;
        TeleportNetwork.send(player, new TeleportVisualPacket(java.util.UUID.randomUUID(), subject.getUUID(),
                TeleportSubjects.anchor(living), 0, 0, TeleportVisualPacket.Kind.DEBUG, true, false, freeze));
        return 1;
    }
}

