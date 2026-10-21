package io.github.kxyr0.echonpc.forge.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.service.SpawnNpcRequest;
import io.github.kxyr0.echonpc.api.service.TeleportNpcRequest;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import io.github.kxyr0.echonpc.core.definition.NpcCatalogState;
import io.github.kxyr0.echonpc.forge.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echonpc.forge.host.ForgeNpcHost;
import io.github.kxyr0.echonpc.forge.host.ForgeWorldIds;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

final class EchoNpcCommandActions {
    private final NpcCatalogState catalogState;
    private final InMemoryDiagnosticBuffer diagnostics;
    private final Supplier<Optional<ForgeNpcHost>> host;

    EchoNpcCommandActions(NpcCatalogState catalogState, InMemoryDiagnosticBuffer diagnostics,
            Supplier<Optional<ForgeNpcHost>> host) {
        this.catalogState = catalogState;
        this.diagnostics = diagnostics;
        this.host = host;
    }

    int health(CommandContext<CommandSourceStack> context) {
        var current = catalogState.current();
        var health = host.get().map(value -> value.service().health()).orElse(null);
        int actors = health == null ? 0 : health.activeActorCount();
        int durable = health == null ? 0 : health.durableActorCount();
        int queued = health == null ? 0 : health.queuedOperationCount();
        send(context, "Echo NPC: active=" + host.get().isPresent() + ", catalogRevision=" + current.revision()
                + ", definitions=" + current.definitions().size() + ", actors=" + actors
                + ", durableActors=" + durable + ", queuedOperations=" + queued
                + ", warnings=" + diagnostics.hasWarnings() + ", errors=" + diagnostics.hasErrors());
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    int definitions(CommandContext<CommandSourceStack> context) {
        catalogState.current().summaries(50).forEach(summary -> send(context, summary.definitionId()
                + " display=\"" + summary.displayName() + "\" skin=" + summary.skinId().isPresent()
                + " defaultForm=" + summary.defaultFormId().map(Object::toString).orElse("<none>")
                + " fingerprint=" + summary.fingerprint().abbreviation()));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    int list(CommandContext<CommandSourceStack> context) {
        var definitions = catalogState.current().definitions().keySet().stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        send(context, "NPC definitions (definitionId): " + definitions.size());
        definitions.forEach(id -> send(context, "  " + id));
        host.get().ifPresentOrElse(value -> {
            var actors = value.service().listActors(200).actors();
            send(context, "NPC actors (actorId): " + actors.size());
            actors.forEach(actor -> send(context, "  " + actor.actorId() + " -> " + actor.definitionId()
                    + " [" + actor.status() + "]"));
        }, () -> send(context, "NPC actors: Echo NPC host недоступен."));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    int actors(CommandContext<CommandSourceStack> context) {
        host.get().ifPresentOrElse(value -> value.service().listActors(50).actors().forEach(actor -> send(context,
                actor.actorId() + " definition=" + actor.definitionId() + " world=" + actor.worldId()
                        + " pos=" + actor.position() + " form="
                        + actor.activeFormId().map(Object::toString).orElse("<none>")
                        + " status=" + actor.status())),
                () -> send(context, "Echo NPC host недоступен."));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    int actor(CommandContext<CommandSourceStack> context) {
        return withActorId(context, actorId -> {
            host.get().ifPresentOrElse(value -> send(context, value.service().query(actorId).toString()),
                    () -> send(context, "Echo NPC host недоступен."));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        });
    }

    int spawnHere(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer)) {
            send(context, "Консоль должна указать координаты spawn.");
            return 0;
        }
        return spawn(context, context.getSource().getPosition());
    }

    int spawnAt(CommandContext<CommandSourceStack> context) {
        return spawn(context, Vec3Argument.getVec3(context, "pos"));
    }

    int teleport(CommandContext<CommandSourceStack> context) {
        return withActorId(context, actorId -> {
            Vec3 pos = Vec3Argument.getVec3(context, "pos");
            var request = new TeleportNpcRequest(actorId, ForgeWorldIds.fromLevel(context.getSource().getLevel()),
                    new NpcPosition(pos.x, pos.y, pos.z), rotation(context.getSource()));
            host.get().ifPresentOrElse(value -> send(context,
                    EchoNpcCommandFormatting.mutation(value.service().teleport(request))),
                    () -> send(context, "Echo NPC host недоступен."));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        });
    }

    int despawn(CommandContext<CommandSourceStack> context) {
        return withActorId(context, actorId -> {
            host.get().ifPresentOrElse(value -> send(context,
                    EchoNpcCommandFormatting.mutation(value.service().despawn(actorId))),
                    () -> send(context, "Echo NPC host недоступен."));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        });
    }

    int diagnostics(CommandContext<CommandSourceStack> context) {
        int limit = context.getNodes().stream().anyMatch(node -> "limit".equals(node.getNode().getName()))
                ? com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "limit") : 20;
        diagnostics.newest(limit).forEach(item -> send(context, item.toString()));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    int operations(CommandContext<CommandSourceStack> context) {
        host.get().ifPresentOrElse(value -> value.service().listOperations(50).operations()
                .forEach(item -> send(context, EchoNpcCommandFormatting.operation(item))),
                () -> send(context, "Echo NPC host недоступен."));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    int operation(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "operationId");
        host.get().ifPresentOrElse(value -> send(context, value.service().operation(id).operation()
                .map(EchoNpcCommandFormatting::operation).orElse("Операция не найдена.")),
                () -> send(context, "Echo NPC host недоступен."));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    int blocked(CommandContext<CommandSourceStack> context) {
        host.get().ifPresentOrElse(value -> value.service().blockedActors(50).actors()
                .forEach(actor -> send(context, actor.actorId() + " status=" + actor.status()
                        + " reason=" + actor.blockedReason().orElse(""))),
                () -> send(context, "Echo NPC host недоступен."));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    int recoveryRetry(CommandContext<CommandSourceStack> context) {
        return withActorId(context, actorId -> {
            host.get().ifPresentOrElse(value -> send(context,
                    EchoNpcCommandFormatting.mutation(value.service().retryRecovery(actorId))),
                    () -> send(context, "Echo NPC host недоступен."));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        });
    }

    int persistenceStatus(CommandContext<CommandSourceStack> context) {
        host.get().ifPresentOrElse(value -> {
            var health = value.service().health();
            send(context, "Persistence: active=" + health.active() + ", durableActors="
                    + health.durableActorCount() + ", blockedActors=" + health.blockedActorCount()
                    + ", queuedOperations=" + health.queuedOperationCount() + ", degraded=" + health.degraded());
        }, () -> send(context, "Echo NPC host недоступен."));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private int spawn(CommandContext<CommandSourceStack> context, Vec3 pos) {
        try {
            var request = new SpawnNpcRequest(actorId(context), new NpcDefinitionId(definitionId(context)),
                    ForgeWorldIds.fromLevel(context.getSource().getLevel()), new NpcPosition(pos.x, pos.y, pos.z),
                    rotation(context.getSource()));
            host.get().ifPresentOrElse(value -> send(context,
                    EchoNpcCommandFormatting.mutation(value.service().spawn(request))),
                    () -> send(context, "Echo NPC host недоступен."));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        } catch (IllegalArgumentException exception) {
            send(context, "Неверный запрос: " + exception.getMessage());
            return 0;
        }
    }

    private int withActorId(CommandContext<CommandSourceStack> context,
            java.util.function.Function<ActorId, Integer> action) {
        try {
            return action.apply(actorId(context));
        } catch (IllegalArgumentException exception) {
            send(context, "Неверный ActorId: " + exception.getMessage());
            return 0;
        }
    }

    private ActorId actorId(CommandContext<CommandSourceStack> context) {
        return new ActorId(ResourceLocationArgument.getId(context, "actorId").toString());
    }

    private String definitionId(CommandContext<CommandSourceStack> context) {
        return ResourceLocationArgument.getId(context, "definitionId").toString();
    }

    private NpcRotation rotation(CommandSourceStack source) {
        Vec2 rotation = source.getRotation();
        return new NpcRotation(rotation.y, rotation.x);
    }

    private void send(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendSuccess(() -> Component.literal(message), false);
    }
}
