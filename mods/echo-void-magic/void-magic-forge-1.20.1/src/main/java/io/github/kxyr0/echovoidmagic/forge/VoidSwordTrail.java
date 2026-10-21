package io.github.kxyr0.echovoidmagic.forge;

import io.github.kxyr0.echovoidmagic.forge.combat.VoidSwordParticleEffects;
import io.github.kxyr0.echovoidmagic.forge.combat.VoidSwordClientParticles;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EchoVoidMagicMod.MOD_ID, value = Dist.CLIENT)
public final class VoidSwordTrail {
    private static final Map<Integer, TrailState> STATES = new HashMap<>();

    private VoidSwordTrail() { }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) {
            STATES.clear();
            return;
        }
        Set<Integer> active = new HashSet<>();
        for (Player player : minecraft.level.players()) {
            if (update(player)) active.add(player.getId());
        }
        STATES.keySet().removeIf(id -> !active.contains(id));
    }

    static List<Vec3> bladePoints(Vec3 eye, Vec3 forwardValue, Vec3 upValue, double progress) {
        return VoidSwordParticleEffects.bladePoints(eye, forwardValue, upValue, progress);
    }

    private static boolean update(Player player) {
        if (!player.swinging || player.swingingArm != InteractionHand.MAIN_HAND
                || !VoidSwordParticleEffects.holdsVoidSword(player)) {
            STATES.remove(player.getId());
            return false;
        }
        TrailState previous = STATES.get(player.getId());
        if (previous != null && previous.swingTime() == player.swingTime) return true;
        double progress = player.getAttackAnim(1.0F);
        List<Vec3> blade = bladePoints(player.getEyePosition(), player.getViewVector(1.0F),
                player.getUpVector(1.0F), progress);
        blade.forEach(VoidSwordClientParticles::spawn);
        Vec3 tip = blade.get(blade.size() - 1);
        if (previous != null && player.swingTime > previous.swingTime()) {
            VoidSwordClientParticles.spawnSweep(previous.tip(), tip);
        }
        STATES.put(player.getId(), new TrailState(player.swingTime, tip));
        return true;
    }

    private record TrailState(int swingTime, Vec3 tip) { }
}
