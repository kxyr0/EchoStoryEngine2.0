package io.github.kxyr0.echoteleportmagic.forge.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import io.github.kxyr0.echoteleportmagic.forge.client.ClientTeleportVisual;
import io.github.kxyr0.echoteleportmagic.forge.client.TeleportClient;
import io.github.kxyr0.echoteleportmagic.timeline.TeleportTimeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "echo_teleport_magic", value = Dist.CLIENT)
public final class TeleportWorldRenderer {
    private static final TeleportQuads QUADS = new TeleportQuads();
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(new BufferBuilder(262144));
    public static final Metrics METRICS = new Metrics();
    public static final class Metrics { public long renderNanos; public int renderedParticles; }
    private TeleportWorldRenderer() { }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (!TeleportClient.hasVisuals()) { METRICS.renderNanos = 0; METRICS.renderedParticles = 0; return; }
        long start = System.nanoTime();
        METRICS.renderedParticles = 0;
        var camera = event.getCamera();
        QUADS.right.set(1, 0, 0).rotate(camera.rotation());
        QUADS.up.set(0, 1, 0).rotate(camera.rotation());
        QUADS.matrix = event.getPoseStack().last().pose();
        QUADS.vertices = BUFFERS.getBuffer(TeleportRenderTypes.FIELD);
        try {
            for (var visual : TeleportClient.active()) {
                visual.rendered = 0;
                var a = visual.anchor;
                if (camera.getPosition().distanceToSqr(a.x(), a.y(), a.z()) > 256 * 256) continue;
                if (!event.getFrustum().isVisible(visual.bounds)) continue;
                field(visual, visual.time(event.getPartialTick()), camera.getPosition());
            }
        } finally { BUFFERS.endBatch(TeleportRenderTypes.FIELD); }
        METRICS.renderNanos = System.nanoTime() - start;
    }
    private static void field(ClientTeleportVisual visual, double t, Vec3 camera) {
        if (t < 0 || t >= 4) return;
        int skip = camera.distanceToSqr(visual.anchor.x(), visual.anchor.y(), visual.anchor.z()) > 96 * 96 ? 3 : 1;
        visual.samples.update(t);
        for (int i = 0; i < visual.field.particles().size(); i += skip) {
            var orb = visual.field.particles().get(i);
            if (!orb.alive(t)) continue;
            float alpha = orb.alpha(t);
            if (alpha <= .001f) continue;
            double ox = Double.isNaN(visual.originX[i]) ? visual.anchor.x() : visual.originX[i];
            double oy = Double.isNaN(visual.originX[i]) ? visual.anchor.y() : visual.originY[i];
            double oz = Double.isNaN(visual.originX[i]) ? visual.anchor.z() : visual.originZ[i];
            double x = ox + visual.samples.value(i, 0) - camera.x;
            double y = oy + visual.samples.value(i, 1) - camera.y;
            double z = oz + visual.samples.value(i, 2) - camera.z;
            float size = visual.samples.value(i, 3);
            if (orb.kind() == 1) {
                QUADS.billboard(x, y, z, size * 2.6f, 1, 1, 1, alpha, 4);
            } else QUADS.billboard(x, y, z, size, 1, 1, 1, alpha, orb.kind() == 2 ? 2 : 0);
            visual.rendered++; METRICS.renderedParticles++;
        }
        core(visual, t, camera);
    }
    private static void core(ClientTeleportVisual visual, double t, Vec3 camera) {
        var a = visual.anchor;
        double x = a.x() - camera.x, y = a.y() - camera.y, z = a.z() - camera.z;
        float scale = Math.max(a.width(), a.height() * .36f);
        float flash = (visual.packet.arrival() ? TeleportTimeline.arrival(t) : TeleportTimeline.flash(t)) * .16f;
        float ground = visual.packet.arrival() ? TeleportTimeline.arrival(t) : TeleportTimeline.ground(t);
        double groundPlane = visual.groundY - camera.y;
        QUADS.ground(x, groundPlane, z, scale * 2.2f, 1, .68f, 0, ground * .3f);
        QUADS.ground(x + scale * .13, groundPlane + .002, z, scale * 1.4f, 1, .9f, 0, ground * .2f);
        for (int i = 0; i < 5; i++) {
            double cy = y + a.height() * (.12 + i * .17);
            float radius = scale * (i == 0 ? .95f : .85f);
            QUADS.billboard(x, cy, z, radius * 1.75f, 1, .68f, 0, flash * .35f, 0);
            QUADS.billboard(x, cy, z, radius, 1, .95f, 0, flash, 1);
        }
    }
}

