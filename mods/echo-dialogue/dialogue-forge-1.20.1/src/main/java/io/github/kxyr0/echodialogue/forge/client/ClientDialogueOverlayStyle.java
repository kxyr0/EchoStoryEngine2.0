package io.github.kxyr0.echodialogue.forge.client;

import io.github.kxyr0.echodialogue.forge.client.config.DialogueOverlayStyleConfig;
import io.github.kxyr0.echodialogue.forge.client.config.DialogueOverlayStyleConfigSource;

/**
 * Visual tuning for the compact dialogue overlay.
 */
public final class ClientDialogueOverlayStyle {
    private static final DialogueOverlayStyleConfigSource SOURCE = new DialogueOverlayStyleConfigSource();

    private ClientDialogueOverlayStyle() {
    }

    public static void refresh() {
        SOURCE.refresh();
    }

    public static DialogueOverlayStyleConfig current() {
        return SOURCE.current();
    }
}
