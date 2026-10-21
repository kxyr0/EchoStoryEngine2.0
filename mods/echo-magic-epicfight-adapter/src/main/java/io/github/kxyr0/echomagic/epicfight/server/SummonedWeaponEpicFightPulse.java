package io.github.kxyr0.echomagic.epicfight.server;

import com.mojang.datafixers.util.Pair;
import io.github.kxyr0.echomagic.epicfight.EchoMagicEpicFightMod;
import io.github.kxyr0.echomagic.forge.api.summoned.SummonedWeaponData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@Mod.EventBusSubscriber(modid = EchoMagicEpicFightMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SummonedWeaponEpicFightPulse {
    private static final Map<UUID, State> STATES = new HashMap<>();
    private SummonedWeaponEpicFightPulse() { }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        ItemStack stack = player.getMainHandItem();
        if (!SummonedWeaponData.isSummoned(stack)) { STATES.remove(player.getUUID()); return; }
        PlayerPatch<?> patch = EpicFightCapabilities.getPlayerPatch(player);
        Pair<AnimationPlayer, AttackAnimation> attack = patch == null || !patch.isEpicFightMode()
                || !patch.getEntityState().attacking() ? null : patch.getAnimator().findFor(AttackAnimation.class);
        if (attack == null) { STATES.remove(player.getUUID()); return; }
        State current = new State(attack.getSecond().getId(), attack.getFirst().getElapsedTime());
        State previous = STATES.put(player.getUUID(), current);
        if (previous == null || previous.animationId() != current.animationId()
                || current.elapsed() + 0.001F < previous.elapsed()) {
            SummonedWeaponData.recordAttackPulse(stack);
        }
    }

    private record State(int animationId, float elapsed) { }
}
