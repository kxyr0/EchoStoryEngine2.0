package io.github.kxyr0.echobackgroundnpc.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.runtime.BackgroundAgentState;
import io.github.kxyr0.echobackgroundnpc.forge.runtime.ForgeBackgroundService;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class BackgroundNpcCommands {
    private final Supplier<ForgeBackgroundService> services;
    public BackgroundNpcCommands(Supplier<ForgeBackgroundService> services) { this.services = services; }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_background_npc").requires(source -> source.hasPermission(2))
                .then(Commands.literal("list").executes(context -> list(context.getSource(), service().agents())))
                .then(Commands.literal("nearby").executes(context -> nearby(context.getSource())))
                .then(Commands.literal("materialized").executes(context -> materialized(context.getSource())))
                .then(Commands.literal("performance").executes(context -> line(context.getSource(),
                        service().performanceReport())))
                .then(Commands.literal("reload").executes(context -> reload(context.getSource())))
                .then(Commands.literal("inspect").then(Commands.argument("id", StringArgumentType.word())
                        .executes(context -> inspect(context.getSource(), StringArgumentType.getString(context, "id")))))
                .then(Commands.literal("schedule").then(Commands.argument("id", StringArgumentType.word())
                        .executes(context -> schedule(context.getSource(), StringArgumentType.getString(context, "id"))))));
    }

    private int list(CommandSourceStack source, List<BackgroundAgentState> values) {
        values.stream().sorted(Comparator.comparing(value -> value.id().value())).forEach(value -> line(source,
                value.id() + " task=" + value.task().kind() + " " + value.materialization()
                        + " phase=" + value.schedulePhase()));
        return values.size();
    }
    private int nearby(CommandSourceStack source) {
        return list(source, service().nearby(source.getLevel(), source.getPosition(), 32.0));
    }
    private int materialized(CommandSourceStack source) {
        return list(source, service().agents().stream().filter(value -> value.materialization().name()
                .equals("MATERIALIZED")).toList());
    }
    private int inspect(CommandSourceStack source, String raw) {
        try {
            return service().find(new BackgroundNpcId(raw)).map(value -> line(source, inspect(value)))
                    .orElseGet(() -> line(source, "BG_NPC_DEFINITION_NOT_FOUND: " + raw));
        } catch (IllegalArgumentException exception) {
            return line(source, "Некорректный BackgroundNpcId: " + raw);
        }
    }
    private String inspect(BackgroundAgentState value) {
        return "id=" + value.id() + " appearance=" + value.appearance() + " state=" + value.materialization()
                + " lod=" + value.lod() + " task=" + value.task() + " target=" + value.target().orElse(null)
                + " phase=" + value.schedulePhase() + " progress=" + value.routeProgress()
                + " owner=" + value.owner() + " revision=" + value.revision();
    }
    private int schedule(CommandSourceStack source, String raw) {
        try {
            BackgroundNpcId id = new BackgroundNpcId(raw);
            if (service().find(id).isEmpty()) return line(source, "BG_NPC_DEFINITION_NOT_FOUND: " + raw);
            service().schedule(id).forEach(entry -> line(source, String.format(java.util.Locale.ROOT,
                    "%02d:%02d %s source=%s", entry.minuteOfDay() / 60, entry.minuteOfDay() % 60,
                    entry.task(), entry.source())));
            return service().schedule(id).size();
        } catch (IllegalArgumentException exception) {
            return line(source, "Некорректный BackgroundNpcId: " + raw);
        }
    }
    private int reload(CommandSourceStack source) {
        service().reload();
        return line(source, "Echo Background NPC reload запущен.");
    }
    private int line(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
    private ForgeBackgroundService service() {
        ForgeBackgroundService service = services.get();
        if (service == null) throw new IllegalStateException("Echo Background NPC server runtime is unavailable.");
        return service;
    }
}
