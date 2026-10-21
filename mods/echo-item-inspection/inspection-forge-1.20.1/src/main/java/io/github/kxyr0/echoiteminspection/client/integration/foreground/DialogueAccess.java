package io.github.kxyr0.echoiteminspection.client.integration.foreground;

import io.github.kxyr0.echodialogue.forge.client.ClientDialogueClient;

final class DialogueAccess {
    private DialogueAccess() { }
    static boolean active() { return ClientDialogueClient.hudSuppressed(); }
}
