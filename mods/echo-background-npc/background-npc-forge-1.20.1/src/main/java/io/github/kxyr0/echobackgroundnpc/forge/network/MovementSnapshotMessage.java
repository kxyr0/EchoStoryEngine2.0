package io.github.kxyr0.echobackgroundnpc.forge.network;

import io.github.kxyr0.echobackgroundnpc.api.task.TaskKind;
import io.github.kxyr0.echobackgroundnpc.core.render.MovementSnapshot;
import io.github.kxyr0.echobackgroundnpc.forge.entity.BackgroundNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

record MovementSnapshotMessage(int entityId, double x, double y, double z, double velocityX,
        double velocityY, double velocityZ, float facing, TaskKind task, long revision, long serverTick) {
    static MovementSnapshotMessage from(BackgroundNpcEntity entity, MovementSnapshot value) {
        return new MovementSnapshotMessage(entity.getId(), value.position().x(), value.position().y(),
                value.position().z(), value.velocityX(), value.velocityY(), value.velocityZ(), value.facing(),
                value.task(), value.revision(), value.serverTick());
    }
    void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId); buffer.writeDouble(x); buffer.writeDouble(y); buffer.writeDouble(z);
        buffer.writeFloat((float) velocityX); buffer.writeFloat((float) velocityY); buffer.writeFloat((float) velocityZ);
        buffer.writeFloat(facing); buffer.writeEnum(task); buffer.writeVarLong(revision); buffer.writeVarLong(serverTick);
    }
    static MovementSnapshotMessage decode(FriendlyByteBuf buffer) {
        return new MovementSnapshotMessage(buffer.readVarInt(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readEnum(TaskKind.class), buffer.readVarLong(), buffer.readVarLong());
    }
    void apply() {
        if (Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getEntity(entityId) instanceof BackgroundNpcEntity entity) {
            entity.acceptSnapshot(x, y, z, velocityX, velocityY, velocityZ, facing, task.name(), revision, serverTick);
        }
    }
}
