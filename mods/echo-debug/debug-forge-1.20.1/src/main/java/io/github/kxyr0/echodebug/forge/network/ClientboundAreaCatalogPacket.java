package io.github.kxyr0.echodebug.forge.network;

import io.github.kxyr0.echodebug.forge.client.EchoDebugClient;
import io.github.kxyr0.echotrigger.api.authoring.catalog.AreaGeometrySnapshot;
import io.github.kxyr0.echotrigger.api.authoring.catalog.TriggerAreaDefinitionView;
import io.github.kxyr0.echotrigger.api.geometry.AreaShapeKind;
import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundAreaCatalogPacket(List<TriggerAreaDefinitionView> areas) {
    public ClientboundAreaCatalogPacket {
        areas = List.copyOf(areas);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(areas.size());
        areas.forEach(area -> writeArea(buffer, area));
    }

    public static ClientboundAreaCatalogPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<TriggerAreaDefinitionView> areas = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            areas.add(readArea(buffer));
        }
        return new ClientboundAreaCatalogPacket(areas);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> EchoDebugClient.applyCatalog(areas)));
        context.setPacketHandled(true);
    }

    private static void writeArea(FriendlyByteBuf buffer, TriggerAreaDefinitionView area) {
        buffer.writeUtf(area.triggerId().value());
        buffer.writeUtf(area.areaName());
        buffer.writeUtf(area.dimension());
        buffer.writeLong(area.definitionRevision().value());
        writeGeometry(buffer, area.geometry());
    }

    private static TriggerAreaDefinitionView readArea(FriendlyByteBuf buffer) {
        TriggerId id = new TriggerId(buffer.readUtf());
        String areaName = buffer.readUtf();
        String dimension = buffer.readUtf();
        TriggerRevision revision = new TriggerRevision(buffer.readLong());
        return new TriggerAreaDefinitionView(id, areaName, dimension, revision, readGeometry(buffer),
                Optional.empty(), List.of());
    }

    private static void writeGeometry(FriendlyByteBuf buffer, AreaGeometrySnapshot geometry) {
        buffer.writeEnum(geometry.shapeKind());
        writeVector(buffer, geometry.min());
        writeVector(buffer, geometry.max());
        writeVector(buffer, geometry.center());
        buffer.writeBoolean(geometry.radius().isPresent());
        geometry.radius().ifPresent(buffer::writeDouble);
        buffer.writeVarInt(geometry.polygonPoints().size());
        geometry.polygonPoints().forEach(point -> writeVector(buffer, point));
    }

    private static AreaGeometrySnapshot readGeometry(FriendlyByteBuf buffer) {
        AreaShapeKind shape = buffer.readEnum(AreaShapeKind.class);
        Vector3 min = readVector(buffer);
        Vector3 max = readVector(buffer);
        Vector3 center = readVector(buffer);
        OptionalDouble radius = buffer.readBoolean() ? OptionalDouble.of(buffer.readDouble())
                : OptionalDouble.empty();
        return new AreaGeometrySnapshot(shape, min, max, center, radius, readVectors(buffer));
    }

    private static List<Vector3> readVectors(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<Vector3> vectors = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            vectors.add(readVector(buffer));
        }
        return List.copyOf(vectors);
    }

    private static void writeVector(FriendlyByteBuf buffer, Vector3 vector) {
        buffer.writeDouble(vector.X());
        buffer.writeDouble(vector.Y());
        buffer.writeDouble(vector.Z());
    }

    private static Vector3 readVector(FriendlyByteBuf buffer) {
        return new Vector3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
