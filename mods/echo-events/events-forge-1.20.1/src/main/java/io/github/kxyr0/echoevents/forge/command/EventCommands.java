package io.github.kxyr0.echoevents.forge.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echoevents.api.event.EventEnvelope;
import io.github.kxyr0.echoevents.api.identity.EventIds.EventOccurrenceId;
import io.github.kxyr0.echoevents.core.runtime.EventDispatcher;
import io.github.kxyr0.echoevents.forge.api.EchoEvents;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class EventCommands {
    private final Map<UUID, Watch> watchers = new HashMap<>();

    public void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("echo_events").requires(source -> source.hasPermission(2))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("history").executes(context -> history(context.getSource())))
                .then(Commands.literal("subscriptions").executes(context -> subscriptions(context.getSource())))
                .then(Commands.literal("inspect").then(Commands.argument("id", StringArgumentType.word())
                        .executes(context -> inspect(context.getSource(), StringArgumentType.getString(context, "id")))))
                .then(Commands.literal("watch").executes(context -> toggle(context.getSource(), Optional.empty()))
                        .then(Commands.argument("kind", StringArgumentType.word()).suggests((context, builder) -> {
                            for (var kind : EventKind.values()) builder.suggest(kind.predicate());
                            return builder.buildFuture();
                        }).executes(context -> toggle(context.getSource(), Optional.of(
                                StringArgumentType.getString(context, "kind")))))));
    }
    public void clear() { watchers.clear(); }
    public void watch(MinecraftServer server, EventDispatcher runtime) {
        watchers.entrySet().removeIf(entry -> server.getPlayerList().getPlayer(entry.getKey()) == null);
        List<EventEnvelope<?>> history = runtime.history();
        for (var entry : watchers.entrySet()) {
            var player = server.getPlayerList().getPlayer(entry.getKey());
            var state = entry.getValue();
            int start = 0;
            for (int position = 0; position < history.size(); position++) {
                if (history.get(position).id().value().equals(state.cursor)) start = position + 1;
            }
            for (int position = Math.max(start, history.size() - 20); position < history.size(); position++) {
                var occurrence = history.get(position);
                if (state.kind.isEmpty() || state.kind.get().equals(occurrence.kind().predicate())) {
                    player.sendSystemMessage(Component.literal(describe(occurrence)));
                    runtime.journalImage().deliveries().values().stream()
                            .filter(value -> value.event().id().equals(occurrence.id())).limit(8)
                            .forEach(value -> player.sendSystemMessage(Component.literal("[TRIGGER] "
                                    + value.subscription().id().value() + " → " + value.executionToken()
                                    + " " + value.status() + " " + value.diagnostic())));
                }
            }
            if (!history.isEmpty()) state.cursor = history.get(history.size() - 1).id().value();
        }
    }
    private int status(CommandSourceStack source) {
        var runtime = EchoEvents.service(source.getServer());
        return say(source, "Echo Events: pending=" + runtime.pendingCount() + ", durable="
                + runtime.journalImage().occurrences().size() + ", subscriptions=" + runtime.index().subscriptions().size()
                + ", revision=" + runtime.index().revision().value() + ", transientDropped=" + runtime.transientDropped());
    }
    private int history(CommandSourceStack source) {
        var history = EchoEvents.service(source.getServer()).history();
        history.stream().skip(Math.max(0, history.size() - 20)).forEach(value -> say(source, describe(value)));
        return history.size();
    }
    private int subscriptions(CommandSourceStack source) {
        var subscriptions = EchoEvents.service(source.getServer()).index().subscriptions();
        for (var kind : EventKind.values()) {
            long count = subscriptions.stream().filter(value -> value.filter().kind() == kind).count();
            if (count > 0) say(source, kind.predicate() + ": " + count);
        }
        return subscriptions.size();
    }
    private int inspect(CommandSourceStack source, String id) {
        var runtime = EchoEvents.service(source.getServer());
        Optional<EventEnvelope<?>> occurrence = Optional.<EventEnvelope<?>>ofNullable(runtime.journalImage().occurrences().get(new EventOccurrenceId(id)))
                .or(() -> runtime.history().stream().filter(value -> value.id().value().equals(id)).findFirst());
        if (occurrence.isEmpty()) return say(source, "Событие не найдено: " + id);
        say(source, describe(occurrence.get()) + " payload=" + occurrence.get().payload());
        runtime.journalImage().deliveries().values().stream().filter(value -> value.event().id().value().equals(id))
                .limit(20).forEach(value -> say(source, value.subscription().id().value() + " revision="
                        + value.subscription().revision() + " " + value.status() + " attempts=" + value.attempts()
                        + " token=" + value.executionToken() + " " + value.diagnostic()));
        return 1;
    }
    private int toggle(CommandSourceStack source, Optional<String> kind) {
        var player = source.getPlayer();
        if (player == null) return say(source, "Наблюдение доступно игроку.");
        if (kind.isPresent() && java.util.Arrays.stream(EventKind.values()).noneMatch(value -> value.predicate().equals(kind.get()))) {
            return say(source, "Неизвестный тип события: " + kind.get());
        }
        if (kind.isEmpty() && watchers.remove(player.getUUID()) != null) return say(source, "Наблюдение выключено.");
        var history = EchoEvents.service(source.getServer()).history();
        String cursor = history.isEmpty() ? "" : history.get(history.size() - 1).id().value();
        watchers.put(player.getUUID(), new Watch(kind, cursor));
        return say(source, "Наблюдение включено.");
    }
    private static String describe(EventEnvelope<?> event) {
        return "[EVENT] " + event.kind().predicate() + " " + event.id().value() + " resource="
                + event.source().resource().value() + " execution=" + event.source().execution().value()
                + " root=" + event.source().rootExecution().value() + " audience=" + event.source().audience()
                + " tick=" + event.source().gameTick() + " " + event.durability();
    }
    private static int say(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
    private static final class Watch {
        private final Optional<String> kind;
        private String cursor;
        private Watch(Optional<String> kind, String cursor) { this.kind = kind; this.cursor = cursor; }
    }
}
