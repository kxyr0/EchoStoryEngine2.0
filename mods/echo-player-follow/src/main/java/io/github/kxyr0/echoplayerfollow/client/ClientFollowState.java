package io.github.kxyr0.echoplayerfollow.client;

import io.github.kxyr0.echoplayerfollow.network.ClientboundFollowPromptPacket;
import io.github.kxyr0.echoplayerfollow.network.ClientboundFollowStartPacket;
import io.github.kxyr0.echoplayerfollow.shared.FollowConstants;
import java.util.Optional;

public final class ClientFollowState {
    private Prompt prompt;
    private ActiveTarget active;
    private long holdStartedAt = -1L;
    private boolean startRequested;
    private boolean ignoreCancelKeyUntilRelease;
    private String unavailableReason = "waiting";

    public void prompt(ClientboundFollowPromptPacket packet, long nowMillis) {
        if (!packet.available()) {
            unavailableReason = packet.reason();
            prompt = null;
            resetHold();
            return;
        }
        unavailableReason = "";
        String previous = prompt == null ? "" : prompt.actorId();
        prompt = new Prompt(packet.entityId(), packet.actorId(), packet.displayName(),
                packet.distance(), nowMillis);
        if (!previous.equals(packet.actorId())) {
            resetHold();
        }
    }

    public Optional<Prompt> prompt(long nowMillis) {
        if (prompt == null || active != null || nowMillis - prompt.receivedAtMillis() > FollowConstants.PROMPT_TTL_MILLIS) {
            return Optional.empty();
        }
        return Optional.of(prompt);
    }

    public void start(ClientboundFollowStartPacket packet) {
        active = new ActiveTarget(packet.entityId(), packet.actorId(), packet.displayName());
        prompt = null;
        resetHold();
        ignoreCancelKeyUntilRelease = true;
    }

    public void stop() {
        active = null;
        prompt = null;
        resetHold();
        ignoreCancelKeyUntilRelease = false;
    }

    public Optional<ActiveTarget> active() {
        return Optional.ofNullable(active);
    }

    public boolean startRequested() {
        return startRequested;
    }

    public void beginHold(long nowMillis) {
        if (holdStartedAt < 0L) {
            holdStartedAt = nowMillis;
        }
    }

    public boolean holdComplete(long nowMillis) {
        return holdStartedAt >= 0L && nowMillis - holdStartedAt >= FollowConstants.HOLD_MILLIS;
    }

    public void markStartRequested() {
        startRequested = true;
    }

    public void resetHold() {
        holdStartedAt = -1L;
        startRequested = false;
    }

    public boolean ignoreCancelKeyUntilRelease() {
        return ignoreCancelKeyUntilRelease;
    }

    public String unavailableReason() {
        return unavailableReason;
    }

    public void waitForProbe() {
        unavailableReason = "waiting";
    }

    public void allowKeyCancellation() {
        ignoreCancelKeyUntilRelease = false;
    }

    public record Prompt(int entityId, String actorId, String displayName, double distance, long receivedAtMillis) {
    }

    public record ActiveTarget(int entityId, String actorId, String displayName) {
    }
}
