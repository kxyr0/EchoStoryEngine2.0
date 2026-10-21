package io.github.kxyr0.echobackgroundnpc.testmod;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundAppearanceId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundLocationId;
import io.github.kxyr0.echobackgroundnpc.api.runtime.AgentLifeState;
import io.github.kxyr0.echobackgroundnpc.api.runtime.BackgroundAgentState;
import io.github.kxyr0.echobackgroundnpc.api.runtime.BackgroundSimulationLod;
import io.github.kxyr0.echobackgroundnpc.api.runtime.MaterializationState;
import io.github.kxyr0.echobackgroundnpc.api.task.BackgroundTask;
import io.github.kxyr0.echobackgroundnpc.api.task.TaskSource;
import io.github.kxyr0.echobackgroundnpc.forge.entity.BackgroundNpcEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;

final class BenchmarkScene {
    private static final List<UUID> ENTITIES = new ArrayList<>();
    private BenchmarkScene() { }

    static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("echo_background_npc_benchmark")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("scene").then(Commands.argument("count", IntegerArgumentType.integer(0, 100))
                        .executes(context -> scene(context.getSource(), IntegerArgumentType.getInteger(context,
                                "count")))))
                .then(Commands.literal("server").then(Commands.argument("ticks", IntegerArgumentType.integer(20, 12000))
                        .executes(context -> ServerTickBenchmark.start(context.getSource(),
                                IntegerArgumentType.getInteger(context, "ticks"))))));
    }

    private static int scene(net.minecraft.commands.CommandSourceStack source, int count) {
        clear(source.getServer().getAllLevels());
        ServerLevel level = source.getLevel();
        var origin = source.getPosition();
        for (int index = 0; index < count; index++) {
            var entity = BackgroundNpcEntities.BACKGROUND_NPC.get().create(level);
            if (entity == null) continue;
            entity.configure(state(index));
            double x = origin.x + index % 10 * 1.8D - 8.1D;
            double z = origin.z + index / 10 * 1.8D + 5.0D;
            entity.setPos(x, origin.y, z);
            if (level.addFreshEntity(entity)) ENTITIES.add(entity.getUUID());
        }
        source.sendSuccess(() -> Component.literal("BG NPC benchmark scene: " + ENTITIES.size()), false);
        return ENTITIES.size();
    }

    private static BackgroundAgentState state(int index) {
        BackgroundNpcId id = new BackgroundNpcId("echo_background_npc_testmod:agent_" + index);
        BackgroundLocationId location = new BackgroundLocationId("echo_background_npc_testmod:scene");
        return new BackgroundAgentState(id, AgentLifeState.ALIVE, MaterializationState.MATERIALIZED,
                BackgroundSimulationLod.NEAR, location, new BackgroundTask.Stay(location), TaskSource.DEBUG,
                new BackgroundAppearanceId("minecraft:steve"), -1, 1.0D, 0L);
    }

    private static void clear(Iterable<ServerLevel> levels) {
        for (ServerLevel level : levels) ENTITIES.forEach(id -> {
            var entity = level.getEntity(id); if (entity != null) entity.discard();
        });
        ENTITIES.clear();
    }
}
