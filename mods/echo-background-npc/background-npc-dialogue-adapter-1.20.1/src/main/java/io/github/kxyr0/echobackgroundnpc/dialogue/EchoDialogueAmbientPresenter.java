package io.github.kxyr0.echobackgroundnpc.dialogue;

import io.github.kxyr0.echobackgroundnpc.api.conversation.AmbientLine;
import io.github.kxyr0.echodialogue.forge.api.client.EchoDialogueTransientLineEvent;
import io.github.kxyr0.echodialogue.forge.api.client.overlay.EchoDialogueTransientClearEvent;
import net.minecraftforge.common.MinecraftForge;

/** Reuses the standard Echo Dialogue renderer without creating a blocking session. */
public final class EchoDialogueAmbientPresenter {
    public void present(String sessionId, AmbientLine line, long durationMillis) {
        MinecraftForge.EVENT_BUS.post(new EchoDialogueTransientLineEvent(sessionId, line.speaker(), line.text(),
                durationMillis));
    }

    public void clear(String sessionId) {
        MinecraftForge.EVENT_BUS.post(new EchoDialogueTransientClearEvent(sessionId));
    }
}
