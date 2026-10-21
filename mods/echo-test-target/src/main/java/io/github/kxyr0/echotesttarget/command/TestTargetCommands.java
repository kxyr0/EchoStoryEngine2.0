package io.github.kxyr0.echotesttarget.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.kxyr0.echotesttarget.entity.ImmortalTestTarget;
import io.github.kxyr0.echotesttarget.registry.TestTargetEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class TestTargetCommands {
    private TestTargetCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_test_target")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("spawn")
                        .executes(context -> spawn(context.getSource(), 1))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(context -> spawn(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "count")))))
                .then(Commands.literal("clear")
                        .executes(context -> clear(context.getSource()))));
    }

    private static int spawn(CommandSourceStack source, int count) {
        ServerLevel level = source.getLevel();
        Vec3 look = source.getEntity() == null ? new Vec3(0.0D, 0.0D, 1.0D)
                : source.getEntity().getLookAngle();
        Vec3 origin = source.getPosition().add(look.scale(3.0D));
        int spawned = 0;
        for (int index = 0; index < count; index++) {
            ImmortalTestTarget target = TestTargetEntities.IMMORTAL_TARGET.get().create(level);
            if (target != null) {
                double offset = (index - (count - 1) * 0.5D) * 0.9D;
                target.moveTo(origin.x + offset, origin.y, origin.z, source.getRotation().y, 0.0F);
                if (level.addFreshEntity(target)) {
                    spawned++;
                }
            }
        }
        int result = spawned;
        source.sendSuccess(() -> Component.translatable("command.echo_test_target.spawned", result), true);
        return result;
    }

    private static int clear(CommandSourceStack source) {
        List<ImmortalTestTarget> targets = new ArrayList<>();
        for (ServerLevel level : source.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ImmortalTestTarget target) {
                    targets.add(target);
                }
            }
        }
        targets.forEach(ImmortalTestTarget::removeForCleanup);
        source.sendSuccess(() -> Component.translatable("command.echo_test_target.cleared", targets.size()), true);
        return targets.size();
    }
}

