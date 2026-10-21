package io.github.kxyr0.echoplayernavigationadapter;

import io.github.kxyr0.echoplayeractor.forge.api.motor.PlayerActorMovementBridgeRegistrationEvent;
import io.github.kxyr0.echoplayernavigationadapter.navigation.PlayerNavigationCommandBridge;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoPlayerNavigationAdapterMod.MOD_ID)
public final class EchoPlayerNavigationAdapterMod {
    public static final String MOD_ID = "echo_player_navigation_adapter";

    public EchoPlayerNavigationAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::registerMovementBridge);
    }

    private void registerMovementBridge(PlayerActorMovementBridgeRegistrationEvent event) {
        event.registry().register(new PlayerNavigationCommandBridge(event.server()));
    }
}
