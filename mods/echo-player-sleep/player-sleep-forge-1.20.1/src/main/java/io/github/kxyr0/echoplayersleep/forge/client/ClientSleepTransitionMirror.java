package io.github.kxyr0.echoplayersleep.forge.client;

import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepRevision;
import io.github.kxyr0.echoplayersleep.forge.network.event.ClientboundEyeTransitionEvent;
import java.util.Optional;

public final class ClientSleepTransitionMirror {
    private static final long MILLIS_PER_TICK = 50L;
    private ActiveTransition active;

    public void onTransition(ClientboundEyeTransitionEvent event) {
        active = new ActiveTransition(event.message().revision(), System.currentTimeMillis(),
                Math.max(1L, event.message().duration().value()) * MILLIS_PER_TICK,
                event.message().closing(), event.message().sleepyBlink(), event.message().showHud());
    }

    public double coverage() {
        if (active == null) {
            return 0.0D;
        }
        long elapsed = System.currentTimeMillis() - active.startedMillis();
        double progress = Math.max(0.0D, Math.min(1.0D, elapsed / (double) active.durationMillis()));
        double eased = progress * progress * (3.0D - 2.0D * progress);
        return active.closing() ? eased : 1.0D - eased;
    }

    public Optional<PlayerSleepRevision> revision() {
        return active == null ? Optional.empty() : Optional.of(active.revision());
    }

    public boolean sleepyBlink() {
        return active != null && active.sleepyBlink();
    }

    public boolean shouldHideHud() {
        if (active == null) {
            return false;
        }
        if (active.closing() || !active.showHud()) {
            return true;
        }
        return coverage() > 0.0D;
    }

    private record ActiveTransition(
            PlayerSleepRevision revision,
            long startedMillis,
            long durationMillis,
            boolean closing,
            boolean sleepyBlink,
            boolean showHud) {
    }
}
