package io.github.kxyr0.echomagic.forge.gesture;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

final class GesturePositionSwap {
    private static final DustParticleOptions VOID_CENTER =
            new DustParticleOptions(new Vector3f(0.08F, 1.0F, 0.18F), 1.15F);
    private static final DustParticleOptions VOID_EDGE =
            new DustParticleOptions(new Vector3f(0.01F, 0.32F, 0.06F), 0.85F);

    private GesturePositionSwap() { }

    static boolean trySwap(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (!validParticipants(player, target)) return fail(player);
        Vec3 playerPosition = player.position();
        Vec3 targetPosition = target.position();
        if (!free(level, player, targetPosition, target)
                || !free(level, target, playerPosition, player)) {
            return fail(player);
        }
        particles(level, playerPosition, targetPosition);
        teleport(level, target, playerPosition);
        player.teleportTo(level, targetPosition.x, targetPosition.y, targetPosition.z,
                player.getYRot(), player.getXRot());
        settle(player);
        settle(target);
        particles(level, player.position(), target.position());
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.9F, 1.25F);
        player.displayClientMessage(Component.translatable("message.echo_magic.swap.success"), true);
        if (target instanceof ServerPlayer targetPlayer) {
            targetPlayer.displayClientMessage(Component.translatable(
                    "message.echo_magic.swap.target"), true);
        }
        return true;
    }

    private static boolean validParticipants(ServerPlayer player, LivingEntity target) {
        return player.isAlive() && target.isAlive() && !target.isRemoved()
                && player.level() == target.level() && !player.isPassenger() && !target.isPassenger()
                && !player.isVehicle() && !target.isVehicle();
    }

    private static boolean free(ServerLevel level, LivingEntity entity, Vec3 destination,
                                LivingEntity counterpart) {
        Vec3 movement = destination.subtract(entity.position());
        AABB bounds = entity.getBoundingBox().move(movement);
        return level.hasChunkAt(net.minecraft.core.BlockPos.containing(destination))
                && level.getWorldBorder().isWithinBounds(bounds)
                && !level.getBlockCollisions(entity, bounds).iterator().hasNext()
                && level.getEntities(entity, bounds, other -> other != counterpart
                        && other.isAlive() && other.canBeCollidedWith()).isEmpty();
    }

    private static void teleport(ServerLevel level, LivingEntity entity, Vec3 destination) {
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(level, destination.x, destination.y, destination.z,
                    player.getYRot(), player.getXRot());
        } else {
            entity.teleportTo(destination.x, destination.y, destination.z);
        }
    }

    private static void settle(LivingEntity entity) {
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0.0F;
        entity.hurtMarked = true;
    }

    private static void particles(ServerLevel level, Vec3 first, Vec3 second) {
        level.sendParticles(VOID_CENTER, first.x, first.y + 0.8D, first.z,
                22, 0.32D, 0.7D, 0.32D, 0.01D);
        level.sendParticles(VOID_CENTER, second.x, second.y + 0.8D, second.z,
                22, 0.32D, 0.7D, 0.32D, 0.01D);
        level.sendParticles(VOID_EDGE, first.x, first.y + 0.8D, first.z,
                12, 0.24D, 0.55D, 0.24D, 0.025D);
        level.sendParticles(VOID_EDGE, second.x, second.y + 0.8D, second.z,
                12, 0.24D, 0.55D, 0.24D, 0.025D);
    }

    private static boolean fail(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.echo_magic.swap.blocked"), true);
        return false;
    }
}
