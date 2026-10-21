package io.github.kxyr0.echobackgroundnpc.testmod;

import io.github.kxyr0.echobackgroundnpc.core.performance.BenchmarkStatistics;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;

final class ServerTickBenchmark {
    private static final List<Long> SAMPLES = new ArrayList<>();
    private static CommandSourceStack source;
    private static int remaining;
    private static long started;
    private ServerTickBenchmark() { }

    static int start(CommandSourceStack commandSource, int ticks) {
        source = commandSource; remaining = ticks; SAMPLES.clear();
        commandSource.sendSuccess(() -> Component.literal("BG NPC server benchmark started: " + ticks + " ticks"),
                false);
        return 1;
    }

    static void tick(TickEvent.ServerTickEvent event) {
        if (remaining <= 0) return;
        if (event.phase == TickEvent.Phase.START) { started = System.nanoTime(); return; }
        SAMPLES.add(System.nanoTime() - started);
        if (--remaining == 0) report();
    }

    private static void report() {
        var result = new BenchmarkStatistics().summarize(SAMPLES.stream().mapToLong(Long::longValue).toArray());
        String text = "BG NPC server tick: n=" + result.samples() + " median="
                + format(result.medianMillis()) + "ms p95=" + format(result.p95Millis()) + "ms";
        source.sendSuccess(() -> Component.literal(text), false);
        source.getServer().sendSystemMessage(Component.literal(text));
        source = null;
    }

    private static String format(double value) { return String.format(java.util.Locale.ROOT, "%.3f", value); }
}
