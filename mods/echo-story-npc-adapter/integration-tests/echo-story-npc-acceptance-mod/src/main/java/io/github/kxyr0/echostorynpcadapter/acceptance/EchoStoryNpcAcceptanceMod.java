package io.github.kxyr0.echostorynpcadapter.acceptance;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceLookupResult;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExecutionBridge;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExecutionLookupResult;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(EchoStoryNpcAcceptanceMod.MOD_ID)
public final class EchoStoryNpcAcceptanceMod {
    public static final String MOD_ID = "echo_story_npc_acceptance";
    private static final Map<String, String> STORIES = Map.of(
            "spawn", "echo_story_npc_test:spawn_father",
            "teleport", "echo_story_npc_test:teleport_father",
            "despawn", "echo_story_npc_test:despawn_father",
            "combined", "echo_story_npc_test:combined_father",
            "dormant", "echo_story_npc_test:spawn_dormant_father",
            "blocked_definition", "echo_story_npc_test:spawn_missing_definition");
    private static final ResourceLocation ECHO_NPC_ENTITY_TYPE = new ResourceLocation("echo_npc", "echo_npc");

    public EchoStoryNpcAcceptanceMod() {
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("echo_story_npc_acceptance")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("start")
                        .then(Commands.argument("scenario", StringArgumentType.word())
                                .executes(context -> start(context, defaultExecution(context)))
                                .then(Commands.argument("executionId", StringArgumentType.greedyString())
                                        .executes(this::startWithExecution))))
                .then(Commands.literal("actor")
                        .then(Commands.argument("actorId", StringArgumentType.greedyString())
                                .executes(this::actor)))
                .then(Commands.literal("entities")
                        .then(Commands.argument("actorId", ResourceLocationArgument.id())
                                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.0D, 128.0D))
                                        .executes(this::entities)))));
    }

    private int startWithExecution(CommandContext<CommandSourceStack> context) {
        return start(context, StringArgumentType.getString(context, "executionId"));
    }

    private int start(CommandContext<CommandSourceStack> context, String rawExecutionId) {
        String scenario = StringArgumentType.getString(context, "scenario");
        String rawStoryId = STORIES.get(scenario);
        if (rawStoryId == null) {
            sendFailure(context, "Неизвестный acceptance scenario: " + scenario);
            return 0;
        }
        EchoStoryExecutionLookupResult lookup = EchoStoryExecutionBridge.lookup(context.getSource().getServer());
        if (lookup.status() != EchoStoryExecutionLookupResult.Status.RESOLVED) {
            sendFailure(context, lookup.message());
            return 0;
        }
        try {
            lookup.starter().orElseThrow().start(StoryId.of(rawStoryId), ExecutionId.of(rawExecutionId));
            sendSuccess(context, "Запущен Story scenario=" + scenario + ", storyId=" + rawStoryId
                    + ", executionId=" + rawExecutionId + ".");
            return Command.SINGLE_SUCCESS;
        } catch (IllegalArgumentException exception) {
            sendFailure(context, "ID должен иметь формат namespace:path.");
            return 0;
        }
    }

    private int actor(CommandContext<CommandSourceStack> context) {
        String rawActorId = StringArgumentType.getString(context, "actorId");
        EchoNpcServiceLookupResult lookup = EchoNpcServiceBridge.lookup(context.getSource().getServer());
        if (lookup.status() != EchoNpcServiceLookupResult.Status.RESOLVED) {
            sendFailure(context, lookup.message());
            return 0;
        }
        try {
            NpcResults.Query result = lookup.service().orElseThrow().query(new ActorId(rawActorId));
            sendSuccess(context, actorLine(rawActorId, result));
            return result.status() == NpcResults.QueryStatus.FOUND ? Command.SINGLE_SUCCESS : 0;
        } catch (IllegalArgumentException exception) {
            sendFailure(context, "ActorId должен иметь формат namespace:path.");
            return 0;
        }
    }

    private String actorLine(String rawActorId, NpcResults.Query result) {
        if (result.status() != NpcResults.QueryStatus.FOUND) {
            return "Actor " + rawActorId + ": " + result.status() + ", " + result.message();
        }
        NpcActorView actor = result.actor().orElseThrow();
        return "Actor " + actor.actorId().value()
                + ": definition=" + actor.definitionId().value()
                + ", display=" + actor.displayName()
                + ", status=" + actor.status()
                + ", desired=" + actor.desiredPresence()
                + ", world=" + actor.worldId().value()
                + ", position=" + position(actor);
    }

    private String position(NpcActorView actor) {
        return actor.position().x() + "," + actor.position().y() + "," + actor.position().z();
    }

    private int entities(CommandContext<CommandSourceStack> context) {
        String rawActorId = ResourceLocationArgument.getId(context, "actorId").toString();
        double radius = DoubleArgumentType.getDouble(context, "radius");
        EchoNpcServiceLookupResult lookup = EchoNpcServiceBridge.lookup(context.getSource().getServer());
        if (lookup.status() != EchoNpcServiceLookupResult.Status.RESOLVED) {
            sendFailure(context, lookup.message());
            return 0;
        }
        try {
            NpcResults.Query result = lookup.service().orElseThrow().query(new ActorId(rawActorId));
            if (result.status() != NpcResults.QueryStatus.FOUND) {
                sendFailure(context, "Actor " + rawActorId + ": " + result.status() + ".");
                return 0;
            }
            int count = entityCount(context.getSource().getLevel(), result.actor().orElseThrow(), radius);
            sendSuccess(context, "Actor " + rawActorId + ": entities=" + count + ".");
            return count;
        } catch (IllegalArgumentException exception) {
            sendFailure(context, "ActorId должен иметь формат namespace:path.");
            return 0;
        }
    }

    private int entityCount(ServerLevel level, NpcActorView actor, double radius) {
        AABB bounds = new AABB(actor.position().x(), actor.position().y(), actor.position().z(),
                actor.position().x(), actor.position().y(), actor.position().z()).inflate(radius);
        return level.getEntities((Entity) null, bounds, this::isEchoNpc).size();
    }

    private boolean isEchoNpc(Entity entity) {
        ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return ECHO_NPC_ENTITY_TYPE.equals(type);
    }

    private String defaultExecution(CommandContext<CommandSourceStack> context) {
        return "echo_story_npc_test:" + StringArgumentType.getString(context, "scenario") + "_acceptance";
    }

    private void sendSuccess(CommandContext<CommandSourceStack> context, String line) {
        context.getSource().sendSuccess(() -> Component.literal(line), false);
    }

    private void sendFailure(CommandContext<CommandSourceStack> context, String line) {
        context.getSource().sendFailure(Component.literal(line));
    }
}
