package io.github.kxyr0.echobackgroundnpc.testmod;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.kxyr0.echobackgroundnpc.core.performance.BenchmarkStatistics;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;

final class ClientFrameBenchmark {
    private static final List<Long> SAMPLES = new ArrayList<>();
    private static int remaining;
    private static long previous;
    private ClientFrameBenchmark() { }

    static void register() { MinecraftForge.EVENT_BUS.addListener(ClientFrameBenchmark::commands);
        MinecraftForge.EVENT_BUS.addListener(ClientFrameBenchmark::frame); }

    private static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("echo_background_npc_frame_benchmark")
                .then(Commands.argument("frames", IntegerArgumentType.integer(60, 12000))
                        .executes(context -> start(IntegerArgumentType.getInteger(context, "frames")))));
    }

    private static int start(int frames) {
        remaining = frames; previous = 0L; SAMPLES.clear();
        message("BG NPC frame benchmark started: " + frames + " frames"); return 1;
    }

    private static void frame(RenderLevelStageEvent event) {
        if (remaining <= 0 || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        long now = System.nanoTime();
        if (previous != 0L) SAMPLES.add(now - previous);
        previous = now;
        if (--remaining == 0) report();
    }

    private static void report() {
        var result = new BenchmarkStatistics().summarize(SAMPLES.stream().mapToLong(Long::longValue).toArray());
        message("BG NPC frame time: n=" + result.samples() + " median=" + format(result.medianMillis())
                + "ms p95=" + format(result.p95Millis()) + "ms");
    }

    private static void message(String text) {
        if (Minecraft.getInstance().player != null)
            Minecraft.getInstance().player.displayClientMessage(Component.literal(text), false);
    }
    private static String format(double value) { return String.format(java.util.Locale.ROOT, "%.3f", value); }
}
