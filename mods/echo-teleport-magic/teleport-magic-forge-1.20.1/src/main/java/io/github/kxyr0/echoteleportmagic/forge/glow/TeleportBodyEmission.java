package io.github.kxyr0.echoteleportmagic.forge.glow;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kxyr0.echoteleportmagic.forge.client.TeleportClient;
import io.github.kxyr0.echoteleportmagic.forge.render.TeleportRenderTypes;
import io.github.kxyr0.echoteleportmagic.timeline.TeleportTimeline;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;

public final class TeleportBodyEmission implements MultiBufferSource {
    private static final TeleportBodyEmission CAPTURE = new TeleportBodyEmission();
    private final MultiBufferSource.BufferSource emission = MultiBufferSource.immediate(new BufferBuilder(262144));
    private final Map<RenderType, EmissionVertex> wrappers = new IdentityHashMap<>();
    private final java.util.List<RenderType> usedTypes = new java.util.ArrayList<>(8);
    private MultiBufferSource original;
    private float intensity;
    private boolean capturing;
    private TeleportBodyEmission() { }

    public static MultiBufferSource wrap(Entity entity, MultiBufferSource original, float partial) {
        var visual = TeleportClient.subject(entity.getUUID());
        if (visual == null || CAPTURE.capturing) return original;
        double t = visual.time(partial);
        if (t < 0 || t >= 4) return original;
        float glow = visual.packet.arrival() ? TeleportTimeline.arrival(t) : TeleportTimeline.body(t);
        if (glow <= .001f) return original;
        CAPTURE.original = original;
        CAPTURE.intensity = glow * .25f;
        CAPTURE.capturing = true;
        return CAPTURE;
    }
    @Override public VertexConsumer getBuffer(RenderType type) {
        VertexConsumer base = original.getBuffer(type);
        if (type.format() != DefaultVertexFormat.NEW_ENTITY) return base;
        EmissionVertex wrapper = wrappers.computeIfAbsent(type, ignored -> new EmissionVertex());
        if (!usedTypes.contains(type)) usedTypes.add(type);
        wrapper.base = base;
        wrapper.light = emission.getBuffer(TeleportRenderTypes.BODY);
        wrapper.intensity = intensity;
        return wrapper;
    }
    public static void finish(MultiBufferSource buffers) {
        if (buffers != CAPTURE) return;
        try {
            if (CAPTURE.original instanceof MultiBufferSource.BufferSource original) {
                for (RenderType type : CAPTURE.usedTypes) original.endBatch(type);
            }
            CAPTURE.emission.endBatch(TeleportRenderTypes.BODY);
        } finally { CAPTURE.usedTypes.clear(); CAPTURE.capturing = false; CAPTURE.original = null; }
    }
}
