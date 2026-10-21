package io.github.kxyr0.echomagic.forge.network.gesture;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureTemplateTarget;
import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;
import io.github.kxyr0.echomagic.core.gesture.template.MagicGestureTemplateLibrary;
import io.github.kxyr0.echomagic.forge.network.MagicClientPacketEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;

public record MagicGestureTemplatesPacket(
        Map<MagicGestureTemplateTarget, MagicGestureTemplateLibrary.Template> templates) {
    public MagicGestureTemplatesPacket { templates = Map.copyOf(templates); }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(templates.size());
        templates.values().stream().sorted(java.util.Comparator.comparing(value -> value.target().ordinal()))
                .forEach(template -> {
                    buffer.writeEnum(template.target()); buffer.writeDouble(template.threshold());
                    template.points().forEach(point -> {
                        buffer.writeDouble(point.x()); buffer.writeDouble(point.y());
                    });
                    buffer.writeVarInt(template.variants().size());
                    template.variants().forEach(variant -> variant.forEach(point -> {
                        buffer.writeDouble(point.x()); buffer.writeDouble(point.y());
                    }));
                });
    }

    public static MagicGestureTemplatesPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MagicGestureTemplateTarget.values().length) {
            throw new IllegalArgumentException("Invalid personal gesture template count: " + size);
        }
        Map<MagicGestureTemplateTarget, MagicGestureTemplateLibrary.Template> templates =
                new EnumMap<>(MagicGestureTemplateTarget.class);
        for (int index = 0; index < size; index++) {
            MagicGestureTemplateTarget target = buffer.readEnum(MagicGestureTemplateTarget.class);
            double threshold = buffer.readDouble();
            List<MagicGesturePoint> points = new ArrayList<>(MagicGestureTemplateLibrary.POINT_COUNT);
            for (int pointIndex = 0; pointIndex < MagicGestureTemplateLibrary.POINT_COUNT; pointIndex++) {
                points.add(new MagicGesturePoint(buffer.readDouble(), buffer.readDouble(), pointIndex));
            }
            int variantCount = buffer.readVarInt();
            if (variantCount < 1 || variantCount > MagicGestureTemplateLibrary.SAMPLE_COUNT) {
                throw new IllegalArgumentException("Invalid gesture variant count: " + variantCount);
            }
            List<List<MagicGesturePoint>> variants = new ArrayList<>(variantCount);
            for (int variantIndex = 0; variantIndex < variantCount; variantIndex++) {
                List<MagicGesturePoint> variant = new ArrayList<>(MagicGestureTemplateLibrary.POINT_COUNT);
                for (int pointIndex = 0; pointIndex < MagicGestureTemplateLibrary.POINT_COUNT; pointIndex++) {
                    variant.add(new MagicGesturePoint(buffer.readDouble(), buffer.readDouble(), pointIndex));
                }
                variants.add(variant);
            }
            templates.put(target, new MagicGestureTemplateLibrary.Template(target, points, variants, threshold));
        }
        return new MagicGestureTemplatesPacket(templates);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> MinecraftForge.EVENT_BUS.post(new MagicClientPacketEvent.Templates(this)));
        context.setPacketHandled(true);
    }
}
