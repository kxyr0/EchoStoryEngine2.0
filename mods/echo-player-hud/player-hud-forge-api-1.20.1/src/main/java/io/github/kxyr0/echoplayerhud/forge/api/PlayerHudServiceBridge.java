package io.github.kxyr0.echoplayerhud.forge.api;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runtime bridge for optional adapters that load after the HUD.
 */
public final class PlayerHudServiceBridge {
    private static final AtomicReference<EchoPlayerHudService> SERVICE = new AtomicReference<>();

    private PlayerHudServiceBridge() {
    }

    public static void attach(EchoPlayerHudService service) {
        SERVICE.set(Objects.requireNonNull(service, "service"));
    }

    public static Optional<EchoPlayerHudService> lookup() {
        return Optional.ofNullable(SERVICE.get());
    }
}
