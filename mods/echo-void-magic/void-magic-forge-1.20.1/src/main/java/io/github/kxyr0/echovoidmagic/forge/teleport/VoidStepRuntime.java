package io.github.kxyr0.echovoidmagic.forge.teleport;

import io.github.kxyr0.echomagic.forge.api.event.MagicLifecycleEvent;
import io.github.kxyr0.echomagic.forge.api.teleport.ShortTeleportTargetResolver;
import io.github.kxyr0.echovoidmagic.forge.VoidMagicIds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;

public final class VoidStepRuntime {
    private static final DustParticleOptions VOID_CENTER =
            new DustParticleOptions(new Vector3f(0.08F, 1.0F, 0.18F), 1.05F);
    private static final DustParticleOptions VOID_EDGE =
            new DustParticleOptions(new Vector3f(0.01F, 0.32F, 0.06F), 0.78F);

    public void onMagicCast(MagicLifecycleEvent event) {
        if (!VoidMagicIds.VOID_STEP.equals(event.spellId())) return;
        event.markHandled();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerPlayer player = server == null ? null
                : server.getPlayerList().getPlayer(event.player().uuid());
        if (player == null) return;
        if (player.isPassenger() || player.isVehicle()) {
            blocked(player);
            return;
        }
        ShortTeleportTargetResolver.find(player.serverLevel(), player,
                ShortTeleportTargetResolver.DEFAULT_RANGE).ifPresentOrElse(
                        destination -> teleport(player, destination), () -> blocked(player));
    }

    private static void teleport(ServerPlayer player, Vec3 destination) {
        ServerLevel level = player.serverLevel();
        Vec3 origin = player.position();
        particles(level, origin);
        player.teleportTo(level, destination.x, destination.y, destination.z,
                player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
        particles(level, destination);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.75F, 1.55F);
        player.displayClientMessage(Component.translatable("message.echo_void_magic.step.success"), true);
    }

    private static void particles(ServerLevel level, Vec3 center) {
        level.sendParticles(VOID_CENTER, center.x, center.y + 0.85D, center.z,
                24, 0.32D, 0.72D, 0.32D, 0.015D);
        level.sendParticles(VOID_EDGE, center.x, center.y + 0.85D, center.z,
                16, 0.28D, 0.65D, 0.28D, 0.035D);
    }

    private static void blocked(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.echo_void_magic.step.blocked"), true);
    }
}
