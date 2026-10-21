package io.github.kxyr0.echocutsceneplayeractoradapter;

import io.github.kxyr0.echocutscene.forge.api.event.CutsceneLifecycleEvent;
import io.github.kxyr0.echocutsceneplayeractoradapter.control.CutscenePlayerControlBridge;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoCutscenePlayerActorAdapterMod.MOD_ID)
public final class EchoCutscenePlayerActorAdapterMod {
    public static final String MOD_ID = "echo_cutscene_player_actor_adapter";
    private final CutscenePlayerControlBridge bridge = new CutscenePlayerControlBridge();

    public EchoCutscenePlayerActorAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::cutsceneLifecycle);
        MinecraftForge.EVENT_BUS.addListener(this::playerLoggedOut);
    }

    private void cutsceneLifecycle(CutsceneLifecycleEvent event) {
        bridge.onLifecycle(event);
    }

    private void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        bridge.onPlayerLoggedOut(event.getEntity());
    }
}
