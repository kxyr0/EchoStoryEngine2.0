package io.github.kxyr0.echoiteminspection.client.integration.foreground;

import io.github.kxyr0.echocutscene.forge.client.EchoCutsceneClient;

final class CutsceneAccess {
    private CutsceneAccess() { }
    static boolean active() { return EchoCutsceneClient.hudSuppressed(); }
}
