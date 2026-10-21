package io.github.kxyr0.echonpcinteraction.forge.seat;

import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpcinteraction.api.request.NpcInteractionRequest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public final class EchoNpcSeatAnchorEntity extends Entity {
    private BlockPosition target = new BlockPosition(0, 0, 0);
    private String npcId = "";
    private String commandId = "";
    private int emptyTicks;

    public EchoNpcSeatAnchorEntity(EntityType<? extends EchoNpcSeatAnchorEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setInvisible(true);
    }

    public static EchoNpcSeatAnchorEntity create(ServerLevel level, NpcInteractionRequest request,
            Vec3 passengerPosition, float yaw) {
        EchoNpcSeatAnchorEntity anchor = new EchoNpcSeatAnchorEntity(
                EchoNpcInteractionEntities.SEAT_ANCHOR.get(), level);
        anchor.moveTo(passengerPosition.x(), passengerPosition.y(), passengerPosition.z(), yaw, 0.0F);
        anchor.target = request.target().position();
        anchor.npcId = request.npcId().value();
        anchor.commandId = request.commandId().value();
        return anchor;
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (!level().isClientSide && targetRemoved()) {
            discard();
        }
        if (!level().isClientSide && getPassengers().isEmpty() && ++emptyTicks > 20) {
            discard();
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        target = new BlockPosition(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ"));
        npcId = tag.getString("NpcId");
        commandId = tag.getString("CommandId");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("TargetX", target.x());
        tag.putInt("TargetY", target.y());
        tag.putInt("TargetZ", target.z());
        tag.putString("NpcId", npcId);
        tag.putString("CommandId", commandId);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    private boolean targetRemoved() {
        return level().getBlockState(new net.minecraft.core.BlockPos(target.x(), target.y(), target.z())).isAir();
    }
}
