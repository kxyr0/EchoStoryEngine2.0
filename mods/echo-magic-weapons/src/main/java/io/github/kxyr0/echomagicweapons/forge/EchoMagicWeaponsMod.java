package io.github.kxyr0.echomagicweapons.forge;

import io.github.kxyr0.echomagic.forge.api.event.MagicLifecycleEvent;
import io.github.kxyr0.echomagicweapons.forge.combat.ArsenalRuntime;
import io.github.kxyr0.echomagicweapons.forge.item.ArsenalItems;
import io.github.kxyr0.echomagicweapons.forge.item.WeaponLease;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EchoMagicWeaponsMod.MOD_ID)
public final class EchoMagicWeaponsMod {
    public static final String MOD_ID = "echo_magic_weapons";
    private static final ArsenalRuntime RUNTIME = new ArsenalRuntime();

    public EchoMagicWeaponsMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ArsenalItems.register(modBus);
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::magicCast);
        MinecraftForge.EVENT_BUS.addListener(this::serverTick);
        MinecraftForge.EVENT_BUS.addListener(this::livingDamage);
        MinecraftForge.EVENT_BUS.addListener(this::knockback);
        MinecraftForge.EVENT_BUS.addListener(this::itemToss);
        MinecraftForge.EVENT_BUS.addListener(this::playerLogout);
        MinecraftForge.EVENT_BUS.addListener(this::playerDeath);
        MinecraftForge.EVENT_BUS.addListener(this::serverStarted);
    }

    public static ArsenalRuntime runtime() { return RUNTIME; }
    private void commands(RegisterCommandsEvent event) { new ArsenalCommands().register(event.getDispatcher()); }
    private void magicCast(MagicLifecycleEvent event) { RUNTIME.magicCast(event); }
    private void livingDamage(LivingDamageEvent event) { RUNTIME.livingDamage(event); }
    private void knockback(LivingKnockBackEvent event) { RUNTIME.knockback(event); }
    private void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) RUNTIME.tick(event.getServer());
    }
    private void itemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player
                && WeaponLease.owned(event.getEntity().getItem(), player)) event.setCanceled(true);
    }
    private void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) RUNTIME.playerLeft(player);
    }
    private void playerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) RUNTIME.playerLeft(player);
    }
    private void serverStarted(ServerStartedEvent event) { RUNTIME.reset(); }
}
