package io.github.kxyr0.echonpcinteractionnavigationadapter;

import io.github.kxyr0.echonpcinteraction.forge.api.event.NpcInteractionExecutionPortRegistrationEvent;
import io.github.kxyr0.echonpcinteractionnavigationadapter.navigation.NpcInteractionNavigationPort;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoNpcInteractionNavigationAdapterMod.MOD_ID)
public final class EchoNpcInteractionNavigationAdapterMod {
    public static final String MOD_ID = "echo_npc_interaction_navigation_adapter";

    public EchoNpcInteractionNavigationAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::registerPort);
    }

    private void registerPort(NpcInteractionExecutionPortRegistrationEvent event) {
        event.register(new NpcInteractionNavigationPort());
    }
}
