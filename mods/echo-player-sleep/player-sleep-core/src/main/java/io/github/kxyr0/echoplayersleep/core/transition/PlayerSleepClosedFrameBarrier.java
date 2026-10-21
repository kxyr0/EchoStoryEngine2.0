package io.github.kxyr0.echoplayersleep.core.transition;

import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepRevision;
import java.util.Objects;
import java.util.Optional;

public final class PlayerSleepClosedFrameBarrier {
    private final PlayerSleepRevision expectedRevision;
    private ClientTransitionAck lastAck;

    public PlayerSleepClosedFrameBarrier(PlayerSleepRevision expectedRevision) {
        this.expectedRevision = Objects.requireNonNull(expectedRevision, "expectedRevision");
    }

    public boolean accept(ClientTransitionAck ack) {
        Objects.requireNonNull(ack, "ack");
        if (!expectedRevision.equals(ack.revision())) {
            return false;
        }
        lastAck = ack;
        return closed();
    }

    public boolean closed() {
        return lastAck != null && lastAck.blackFramePublished() && lastAck.coverage() >= 1.0D;
    }

    public Optional<ClientTransitionAck> lastAck() {
        return Optional.ofNullable(lastAck);
    }
}
