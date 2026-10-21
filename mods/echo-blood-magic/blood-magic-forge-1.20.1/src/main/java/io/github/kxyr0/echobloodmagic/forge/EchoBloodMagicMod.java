package io.github.kxyr0.echobloodmagic.forge;

import io.github.kxyr0.echobloodmagic.forge.combat.BloodMagicRuntime;
import io.github.kxyr0.echobloodmagic.forge.item.BloodMagicItems;
import io.github.kxyr0.echobloodmagic.forge.item.BloodRapierLease;
import io.github.kxyr0.echomagic.forge.api.event.MagicLifecycleEvent;
import io.github.kxyr0.echomagic.forge.api.event.MagicLinkCutEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EchoBloodMagicMod.MOD_ID)
public final class EchoBloodMagicMod {
    public static final String MOD_ID = "echo_blood_magic";
    private final BloodMagicRuntime runtime = new BloodMagicRuntime();

    public EchoBloodMagicMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BloodMagicParticles.register(modBus);
        BloodMagicItems.register(modBus);
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::magicCast);
        MinecraftForge.EVENT_BUS.addListener(this::serverTick);
        MinecraftForge.EVENT_BUS.addListener(this::livingDamage);
        MinecraftForge.EVENT_BUS.addListener(this::linkCut);
        MinecraftForge.EVENT_BUS.addListener(this::rightClick);
        MinecraftForge.EVENT_BUS.addListener(this::livingDeath);
        MinecraftForge.EVENT_BUS.addListener(this::itemToss);
        MinecraftForge.EVENT_BUS.addListener(this::playerLogout);
        MinecraftForge.EVENT_BUS.addListener(this::serverStarted);
    }

    private void commands(RegisterCommandsEvent event) {
        new BloodMagicCommands().register(event.getDispatcher());
    }
    private void magicCast(MagicLifecycleEvent event) { runtime.magicCast(event); }
    private void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) runtime.tick(event.getServer());
    }
    private void livingDamage(LivingDamageEvent event) { runtime.livingDamage(event); }
    private void linkCut(MagicLinkCutEvent event) { runtime.linkCut(event); }
    private void rightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && runtime.rightClick(player)) {
            event.setCanceled(true);
        }
    }
    private void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) runtime.playerLeft(player);
    }
    private void livingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) runtime.playerDied(player);
    }
    private void itemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player
                && BloodRapierLease.owned(event.getEntity().getItem(), player)) event.setCanceled(true);
    }
    private void serverStarted(ServerStartedEvent event) { runtime.reset(); }
}
