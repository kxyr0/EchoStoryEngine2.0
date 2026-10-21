package io.github.kxyr0.echoteleportmagic.testmod;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in development measurement; excludes screenshot readback and initial warmup frames. */
@Mod.EventBusSubscriber(modid = "echo_teleport_magic_test", value = Dist.CLIENT)
public final class TeleportRenderProbe {
    private static final long[] ALLOCATIONS = new long[2048], COST = new long[2048], FRAMES = new long[2048];
    private static final com.sun.management.ThreadMXBean BEAN = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static long before, lastFrame;
    private static int count, frames, peak;
    private TeleportRenderProbe() { }

    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void before(RenderLevelStageEvent event) {
        if (!Boolean.getBoolean("etm.smoke") || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        before = BEAN.getThreadAllocatedBytes(Thread.currentThread().getId());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void after(RenderLevelStageEvent event) {
        if (!Boolean.getBoolean("etm.smoke") || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        long allocated = BEAN.getThreadAllocatedBytes(Thread.currentThread().getId()) - before;
        long now = System.nanoTime();
        if (io.github.kxyr0.echoteleportmagic.forge.client.TeleportClient.hasVisuals()) {
            if (++frames > 30 && count < COST.length) {
                var metrics = io.github.kxyr0.echoteleportmagic.forge.render.TeleportWorldRenderer.METRICS;
                ALLOCATIONS[count] = allocated;
                COST[count] = metrics.renderNanos;
                FRAMES[count] = lastFrame == 0 ? 0 : now - lastFrame;
                peak = Math.max(peak, metrics.renderedParticles);
                count++;
            }
        } else frames = 0;
        lastFrame = now;
    }
    public static String report() {
        return "{\"samples\":" + count + ",\"peakRenderedParticles\":" + peak
                + ",\"fieldDrawCalls\":1,\"renderMicrosMean\":" + mean(COST) / 1000.0
                + ",\"renderMicrosP95\":" + percentile(COST) / 1000.0
                + ",\"fieldAllocatedBytesMean\":" + mean(ALLOCATIONS)
                + ",\"frameMillisP95\":" + percentile(FRAMES) / 1000000.0 + "}\n";
    }
    private static double mean(long[] array) {
        long sum = 0;
        for (int i = 0; i < count; i++) sum += array[i];
        return count == 0 ? 0 : (double) sum / count;
    }
    private static long percentile(long[] array) {
        if (count == 0) return 0;
        long[] sorted = Arrays.copyOf(array, count);
        Arrays.sort(sorted);
        return sorted[Math.min(count - 1, (int) (count * .95))];
    }
}
