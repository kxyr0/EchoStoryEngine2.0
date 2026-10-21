package io.github.kxyr0.echonpc.forge.entity;

import io.github.kxyr0.echonpc.core.rest.NpcRestAttachment;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;

/**
 * Owns the synchronized and NBT representation of an NPC rest attachment.
 */
final class EchoNpcRestState {
    private EchoNpcRestState() {
    }

    static void define(SynchedEntityData data) {
        data.define(EchoNpcEntity.SLEEPING, false);
        data.define(EchoNpcEntity.REST_PROVIDER, "");
        data.define(EchoNpcEntity.REST_BED, "");
        data.define(EchoNpcEntity.REST_FACING, "");
    }

    static void apply(SynchedEntityData data, NpcRestAttachment attachment) {
        data.set(EchoNpcEntity.SLEEPING, true);
        data.set(EchoNpcEntity.REST_PROVIDER, attachment.providerId());
        data.set(EchoNpcEntity.REST_BED, attachment.canonicalBedPosition().toString());
        data.set(EchoNpcEntity.REST_FACING, facing(attachment.providerState()).getName());
    }

    static void clear(SynchedEntityData data) {
        data.set(EchoNpcEntity.SLEEPING, false);
        data.set(EchoNpcEntity.REST_PROVIDER, "");
        data.set(EchoNpcEntity.REST_BED, "");
        data.set(EchoNpcEntity.REST_FACING, "");
    }

    static boolean sleeping(SynchedEntityData data) {
        return data.get(EchoNpcEntity.SLEEPING);
    }

    static String provider(SynchedEntityData data) {
        return data.get(EchoNpcEntity.REST_PROVIDER);
    }

    static String bed(SynchedEntityData data) {
        return data.get(EchoNpcEntity.REST_BED);
    }

    static String facing(SynchedEntityData data) {
        return data.get(EchoNpcEntity.REST_FACING);
    }

    static Optional<Direction> orientation(SynchedEntityData data) {
        return sleeping(data) ? Optional.ofNullable(Direction.byName(facing(data))) : Optional.empty();
    }

    static void save(SynchedEntityData data, CompoundTag tag) {
        tag.putBoolean("EchoNpcSleeping", sleeping(data));
        tag.putString("EchoNpcRestProvider", provider(data));
        tag.putString("EchoNpcRestBed", bed(data));
        tag.putString("EchoNpcRestFacing", facing(data));
    }

    static void load(SynchedEntityData data, CompoundTag tag) {
        data.set(EchoNpcEntity.SLEEPING, tag.getBoolean("EchoNpcSleeping"));
        data.set(EchoNpcEntity.REST_PROVIDER, tag.getString("EchoNpcRestProvider"));
        data.set(EchoNpcEntity.REST_BED, tag.getString("EchoNpcRestBed"));
        data.set(EchoNpcEntity.REST_FACING, tag.getString("EchoNpcRestFacing"));
    }

    private static Direction facing(String providerState) {
        for (String entry : providerState.split(";")) {
            if (entry.startsWith("facing=")) {
                Direction direction = Direction.byName(entry.substring("facing=".length()));
                return direction == null ? Direction.SOUTH : direction;
            }
        }
        return Direction.SOUTH;
    }
}
