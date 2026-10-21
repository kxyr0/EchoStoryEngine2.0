package io.github.kxyr0.echomagic.epicfight.client;

import com.mojang.datafixers.util.Pair;
import io.github.kxyr0.echomagic.epicfight.EchoMagicEpicFightMod;
import io.github.kxyr0.echomagic.epicfight.EchoMagicEpicFightParticles;
import io.github.kxyr0.echomagic.forge.api.client.particle.MagicBillboardParticleOptions;
import io.github.kxyr0.echomagic.forge.api.summoned.SummonedWeaponData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@Mod.EventBusSubscriber(modid = EchoMagicEpicFightMod.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SummonedWeaponEpicFightTrail {
    private static final Map<Integer, TrailState> STATES = new HashMap<>();
    private SummonedWeaponEpicFightTrail() { }

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
        ItemStack stack = player.getMainHandItem();
        if (!SummonedWeaponData.isSummoned(stack)) return false;
        PlayerPatch<?> patch = EpicFightCapabilities.getPlayerPatch(player);
        float frameTime = Minecraft.getInstance().getFrameTime();
        OpenMatrix4f hand = EpicFightHandAnchor.resolve(patch, InteractionHand.MAIN_HAND, frameTime);
        OpenMatrix4f offhand = EpicFightHandAnchor.resolve(patch, InteractionHand.OFF_HAND, frameTime);
        if (hand == null) return false;
        Pair<AnimationPlayer, AttackAnimation> attack = patch == null || !patch.isEpicFightMode()
                || !patch.getEntityState().attacking() ? null : patch.getAnimator().findFor(AttackAnimation.class);
        float elapsed = attack == null ? 0.0F : attack.getFirst().getElapsedTime();
        Vec3 tip = SummonedWeaponGeometry.weapon(player, hand, offhand, stack).tip();
        TrailState previous = STATES.get(player.getId());
        int animationId = attack == null ? -1 : attack.getSecond().getId();
        if (attack != null && previous != null && previous.animationId() == animationId
                && elapsed >= previous.elapsed()) spawnSweep(previous.tip(), tip, stack);
        STATES.put(player.getId(), new TrailState(animationId, elapsed, tip));
        return true;
    }

    private static void spawnSweep(Vec3 from, Vec3 to, ItemStack stack) {
        double distance = from.distanceTo(to);
        int steps = Math.max(1, Math.min(14, (int) Math.ceil(distance / 0.06D)));
        for (int index = 1; index <= steps; index++) {
            spawn(from.lerp(to, index / (double) steps), stack, 0.062F, 0.9F, 2);
        }
    }

    private static void spawn(Vec3 point, ItemStack stack, float size, float brightness, int lifetime) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        int color = SummonedWeaponData.color(stack);
        float red = ((color >> 16) & 255) / 255.0F * brightness;
        float green = ((color >> 8) & 255) / 255.0F * brightness;
        float blue = (color & 255) / 255.0F * brightness;
        minecraft.level.addParticle(new MagicBillboardParticleOptions(
                        EchoMagicEpicFightParticles.WEAPON_GLOW.get(), red, green, blue,
                        size, lifetime, 0.96F, 0.12F),
                point.x, point.y, point.z, 0.0D, 0.0D, 0.0D);
    }

    private record TrailState(int animationId, float elapsed, Vec3 tip) { }
}
