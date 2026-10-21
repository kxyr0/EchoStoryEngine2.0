package io.github.kxyr0.echoplayerhud.forge.experience;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ExperienceRemovalEvents {
    @SubscribeEvent
    public void onExperienceOrbJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ExperienceOrb) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onExperiencePickup(PlayerXpEvent.PickupXp event) {
        event.setCanceled(true);
        event.getOrb().discard();
    }

    @SubscribeEvent
    public void onExperienceChange(PlayerXpEvent.XpChange event) {
        event.setAmount(0);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onExperienceLevelChange(PlayerXpEvent.LevelChange event) {
        event.setLevels(0);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        event.setDroppedExperience(0);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        event.setExpToDrop(0);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        clear(event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        clear(event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            clear(event.player);
        }
    }

    private static void clear(Player player) {
        if (player.level().isClientSide || player.experienceLevel == 0
                && player.totalExperience == 0 && player.experienceProgress == 0.0F) {
            return;
        }
        player.experienceLevel = 0;
        player.totalExperience = 0;
        player.experienceProgress = 0.0F;
    }
}
