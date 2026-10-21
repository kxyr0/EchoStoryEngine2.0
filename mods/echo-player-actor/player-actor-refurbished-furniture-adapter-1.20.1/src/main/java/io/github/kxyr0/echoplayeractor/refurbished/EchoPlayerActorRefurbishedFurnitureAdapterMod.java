package io.github.kxyr0.echoplayeractor.refurbished;

import io.github.kxyr0.echoplayeractor.forge.api.seat.PlayerActorSeatProviderRegistrationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoPlayerActorRefurbishedFurnitureAdapterMod.MOD_ID)
public final class EchoPlayerActorRefurbishedFurnitureAdapterMod {
    public static final String MOD_ID = "echo_player_actor_refurbished_furniture_adapter";

    public EchoPlayerActorRefurbishedFurnitureAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::registerSeatProvider);
    }

    private void registerSeatProvider(PlayerActorSeatProviderRegistrationEvent event) {
        if (!ModList.get().isLoaded("refurbished_furniture")) {
            return;
        }
        RefurbishedFurnitureSeatReflection.create()
                .map(RefurbishedFurnitureSeatProvider::new)
                .ifPresent(event.registry()::register);
    }
}
