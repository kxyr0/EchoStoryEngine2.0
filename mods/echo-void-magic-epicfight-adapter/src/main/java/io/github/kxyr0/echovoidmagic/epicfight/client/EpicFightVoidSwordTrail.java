package io.github.kxyr0.echovoidmagic.epicfight.client;

import com.mojang.datafixers.util.Pair;
import io.github.kxyr0.echovoidmagic.epicfight.EchoVoidMagicEpicFightMod;
import io.github.kxyr0.echovoidmagic.forge.combat.VoidSwordParticleEffects;
import io.github.kxyr0.echovoidmagic.forge.combat.VoidSwordClientParticles;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@Mod.EventBusSubscriber(modid = EchoVoidMagicEpicFightMod.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EpicFightVoidSwordTrail {
    private static final Map<Integer, TrailState> STATES = new HashMap<>();
    private EpicFightVoidSwordTrail() { }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) { STATES.clear(); return; }
        Set<Integer> active = new HashSet<>();
        for (Player player : minecraft.level.players()) if (update(player)) active.add(player.getId());
        STATES.keySet().removeIf(id -> !active.contains(id));
    }

    private static boolean update(Player player) {
        if (!VoidSwordParticleEffects.holdsVoidSword(player)) return false;
        PlayerPatch<?> patch = EpicFightCapabilities.getPlayerPatch(player);
        if (patch == null || !patch.isEpicFightMode() || !patch.getEntityState().attacking()) return false;
        Pair<AnimationPlayer, AttackAnimation> animation = patch.getAnimator().findFor(AttackAnimation.class);
        if (animation == null || animation.getSecond().getTotalTime() <= 0.0F) return false;
        float elapsed = animation.getFirst().getElapsedTime();
        double progress = Math.max(0.0D, Math.min(1.0D, elapsed / animation.getSecond().getTotalTime()));
        List<Vec3> blade = VoidSwordParticleEffects.bladePoints(player.getEyePosition(),
                player.getViewVector(1.0F), player.getUpVector(1.0F), progress);
        blade.forEach(VoidSwordClientParticles::spawn);
        Vec3 tip = blade.get(blade.size() - 1);
        TrailState previous = STATES.get(player.getId());
        int animationId = animation.getSecond().getId();
        if (previous != null && previous.animationId() == animationId && elapsed >= previous.elapsed()) {
            VoidSwordClientParticles.spawnSweep(previous.tip(), tip);
        }
        STATES.put(player.getId(), new TrailState(animationId, elapsed, tip));
        return true;
    }

    private record TrailState(int animationId, float elapsed, Vec3 tip) { }
}
