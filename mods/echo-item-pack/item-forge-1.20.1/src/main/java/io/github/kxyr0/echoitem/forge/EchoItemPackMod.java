package io.github.kxyr0.echoitem.forge;

import io.github.kxyr0.echoitem.forge.client.EchoItemPackClient;
import io.github.kxyr0.echoitem.forge.command.EchoItemCommands;
import io.github.kxyr0.echoitem.forge.network.CatalogSyncChannel;
import io.github.kxyr0.echoitem.forge.network.ItemDefinitionSynchronizer;
import io.github.kxyr0.echoitem.forge.reload.EquippedWeaponRefresher;
import io.github.kxyr0.echoitem.forge.reload.ItemPackReloadTicker;
import io.github.kxyr0.echoitem.forge.registry.RegisteredWeaponItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Forge entrypoint for Echo Item Pack.
 */
@Mod(EchoItemPackMod.MOD_ID)
public final class EchoItemPackMod {
    public static final String MOD_ID = "echo_item_pack";
    public static final String MOD_VERSION = "0.2.2";
    private final ForgeItemRuntimeState state;
    private final RegisteredWeaponItems registeredItems;
    private final EquippedWeaponRefresher equippedWeapons;
    private final CatalogSyncChannel catalogSync;
    private final ItemPackReloadTicker reloadTicker;

    public EchoItemPackMod() {
        this.state = ForgeItemRuntimeState.create(FMLPaths.GAMEDIR.get(), getClass().getClassLoader());
        this.registeredItems = new RegisteredWeaponItems(state);
        this.equippedWeapons = new EquippedWeaponRefresher();
        ItemDefinitionSynchronizer synchronizer = new ItemDefinitionSynchronizer(state.catalog());
        this.catalogSync = new CatalogSyncChannel(synchronizer);
        this.reloadTicker = new ItemPackReloadTicker(state, equippedWeapons, catalogSync);
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> EchoItemPackClient.bootstrap(modBus, state));
        modBus.addListener(registeredItems::onRegister);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(reloadTicker::onServerTick);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        new EchoItemCommands(state, registeredItems, equippedWeapons, catalogSync).register(event.getDispatcher());
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            catalogSync.sendTo(player, state.catalog().snapshot());
        }
    }
}
