package io.github.kxyr0.echoobservation.forge.client.input;

import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.api.model.ObservationDefinition;
import io.github.kxyr0.echoobservation.forge.network.ObservationNetwork;
import io.github.kxyr0.echoobservation.forge.network.ObservationPackets;
import java.util.Optional;
import net.minecraft.client.Minecraft;

public final class ObservationFocusInput {
    private static final int HEARTBEAT_INTERVAL = 5;
    private static final int JITTER_GRACE = 3;
    private ObservationDefinition focusing;
    private int startedTick;
    private int lastHeartbeat;
    private int requiredTicks;
    private int lostTicks;
    private boolean accepted;
    private double resolvedProgress;

    public void tick(Optional<ObservationDefinition> target, boolean concentration, int clientTick) {
        boolean held = concentration && Minecraft.getInstance().options.keyInventory.isDown();
        if (!held || target.isEmpty()) {
            if (focusing != null && held && lostTicks++ < JITTER_GRACE) return;
            stop();
            return;
        }
        ObservationDefinition selected = target.orElseThrow();
        if (focusing == null || !focusing.id().equals(selected.id())) {
            stop();
            start(selected, clientTick);
            return;
        }
        lostTicks = 0;
        if (clientTick - lastHeartbeat >= HEARTBEAT_INTERVAL) {
            lastHeartbeat = clientTick;
            send(ObservationPackets.FocusKind.HEARTBEAT, focusing);
        }
    }

    public void accept(ObservationPackets.ClientboundFocus packet, int clientTick) {
        if (packet.status() == ObservationPackets.FocusStatus.COMPLETED) {
            resolvedProgress = 1.0D;
            accepted = false;
            return;
        }
        if (packet.status() == ObservationPackets.FocusStatus.ACCEPTED && focusing != null
                && focusing.id().toString().equals(packet.observationId())) {
            accepted = true;
            requiredTicks = packet.requiredTicks();
            startedTick = clientTick;
            return;
        }
        if (packet.status() == ObservationPackets.FocusStatus.REJECTED) stopLocal();
    }

    public double progress(double clientTick) {
        if (resolvedProgress > 0.0D) return resolvedProgress;
        if (!accepted || requiredTicks < 1) return 0.0D;
        return Math.min(0.99D, Math.max(0.0D, (clientTick - startedTick) / (double) requiredTicks));
    }

    public Optional<ObservationId> focusing() {
        return focusing == null ? Optional.empty() : Optional.of(focusing.id());
    }

    public void clear() {
        stop();
        resolvedProgress = 0.0D;
    }

    private void start(ObservationDefinition definition, int clientTick) {
        focusing = definition;
        startedTick = clientTick;
        lastHeartbeat = clientTick;
        requiredTicks = definition.focus().ticks();
        lostTicks = 0;
        accepted = false;
        resolvedProgress = 0.0D;
        send(ObservationPackets.FocusKind.START, definition);
    }

    private void stop() {
        if (focusing != null) send(ObservationPackets.FocusKind.STOP, focusing);
        stopLocal();
    }

    private void stopLocal() {
        focusing = null;
        accepted = false;
        requiredTicks = 0;
        lostTicks = 0;
        resolvedProgress = 0.0D;
    }

    private void send(ObservationPackets.FocusKind kind, ObservationDefinition definition) {
        ObservationNetwork.sendToServer(new ObservationPackets.ServerboundFocus(
                kind, definition.id().toString(), definition.revision()));
    }
}
