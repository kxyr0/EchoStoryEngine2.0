package io.github.kxyr0.echoiteminspection.client.integration.foreground;

import io.github.kxyr0.echoconcentration.api.ConcentrationServiceBridge;

final class ConcentrationAccess {
    private ConcentrationAccess() { }
    static boolean active() {
        return ConcentrationServiceBridge.lookup().map(service -> service.snapshot().active()).orElse(false);
    }
}
