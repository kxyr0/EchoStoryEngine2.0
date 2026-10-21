package io.github.kxyr0.echoteleportmagic.forge.client;

import io.github.kxyr0.echoteleportmagic.forge.render.TeleportWorldRenderer;
import io.github.kxyr0.echoteleportmagic.timeline.TeleportTimeline;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "echo_teleport_magic", value = Dist.CLIENT)
public final class TeleportScreen {
    private TeleportScreen() { }
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var gui = event.getGuiGraphics();
        int width = gui.guiWidth(), height = gui.guiHeight();
        var visual = TeleportClient.subject(mc.player.getUUID());
        if (visual != null && mc.options.getCameraType().isFirstPerson()) {
            double t = visual.time(event.getPartialTick());
            float exposure = visual.packet.arrival() ? TeleportTimeline.arrival(t) * .9f
                    : TeleportTimeline.smooth(.4, 1.8, t) * .18f
                            + TeleportTimeline.smooth(1.78, 1.93, t) * (1 - TeleportTimeline.smooth(2.02, 2.28, t)) * .82f;
            if (t < 0 || t >= (visual.packet.arrival() ? 1.7 : 3)) exposure = 0;
            int alpha = (int) (Math.min(1, exposure) * 255);
            gui.fill(0, 0, width, height, alpha << 24 | 0xFFDD00);
            // Sparse soft peripheral sprites share the world atlas and deterministic seed.
            for (int i = 0; i < 8; i++) {
                double phase = t * .35 + i * .173;
                int x = (int) (width * (i % 2 == 0 ? .06 + .08 * Math.sin(i) : .85 + .08 * Math.cos(i)));
                int y = (int) (height * (1 - (phase - Math.floor(phase))));
                gui.setColor(1, 1, 1, Math.max(0, Math.min(.8f, exposure + .12f)));
                gui.blit(io.github.kxyr0.echoteleportmagic.forge.render.TeleportRenderTypes.ATLAS,
                        x, y, 14, 14, 0, 0, 128, 128, 640, 128);
            }
            gui.setColor(1, 1, 1, 1);
        }
        if (!TeleportClient.debug()) return;
        int row = 4;
        for (var v : TeleportClient.active()) {
            double t = v.time(event.getPartialTick());
            gui.drawString(mc.font, "Teleport " + v.packet.execution() + " " + TeleportTimeline.phase(t), 4, row, 0xFFFFFF); row += 10;
            gui.drawString(mc.font, String.format(java.util.Locale.ROOT,
                    "t=%.2f progress=%.2f commit=%s glow=%.2f ground=%.2f seed=%d", t, t / 4,
                    v.frozen, TeleportTimeline.body(t), TeleportTimeline.ground(t), v.packet.seed()), 4, row, 0xFFD964); row += 10;
            gui.drawString(mc.font, "source=" + v.anchor + " particles=" + v.field.particles().size()
                    + " rendered=" + v.rendered + " CPU(ns)=" + TeleportWorldRenderer.METRICS.renderNanos, 4, row, 0xFFFFFF); row += 10;
        }
    }
}

