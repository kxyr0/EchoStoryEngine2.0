package io.github.kxyr0.echoplayerfollow.client;

import io.github.kxyr0.echoplayerfollow.client.targeting.FollowTargeting;
import io.github.kxyr0.echoplayerfollow.network.FollowNetwork;
import io.github.kxyr0.echoplayerfollow.network.ServerboundFollowCancelPacket;
import io.github.kxyr0.echoplayerfollow.network.ServerboundFollowProbePacket;
import io.github.kxyr0.echoplayerfollow.network.ServerboundFollowStartPacket;
import io.github.kxyr0.echoplayerfollow.shared.FollowConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;

final class ClientFollowRuntime {
    private final ClientFollowState state;
    private final FollowMovementController movement;
    private String activeActorId = "";
    private long activeStartedAtMillis = -1L;
    private long lastProbeAtMillis = -1L;
    private int lastProbeTargetEntityId = -1;
    private boolean manualInputArmed;
    private boolean wasKeyDown;

    ClientFollowRuntime(ClientFollowState state, FollowMovementController movement) {
        this.state = state;
        this.movement = movement;
    }

    void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        boolean keyDown = FollowKeyMappings.FOLLOW.isDown();
        if (state.ignoreCancelKeyUntilRelease() && !keyDown) {
            state.allowKeyCancellation();
        }
        state.active().ifPresentOrElse(active -> tickActiveSession(active, keyDown), () -> tickInactive(keyDown));
        wasKeyDown = keyDown;
    }

    void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        state.stop();
        movement.release();
        activeActorId = "";
        activeStartedAtMillis = -1L;
        manualInputArmed = false;
    }

    void onMovementInput(MovementInputUpdateEvent event) {
        state.active().ifPresent(active -> movement.tick(active).ifPresent(reason -> cancel(active, reason)));
    }

    private void tickActive(ClientFollowState.ActiveTarget active, boolean keyDown) {
        Minecraft minecraft = Minecraft.getInstance();
        String blockReason = ClientModeGuards.activeBlockReason(minecraft);
        if (!blockReason.isEmpty()) {
            cancel(active, blockReason);
            return;
        }
        if (!state.ignoreCancelKeyUntilRelease() && keyDown && !wasKeyDown) {
            cancel(active, "toggle");
            return;
        }
        String manualReason = ClientModeGuards.manualMovementReason(minecraft);
        boolean graceOver = Util.getMillis() - activeStartedAtMillis >= FollowConstants.MANUAL_CANCEL_GRACE_MILLIS;
        if (!manualReason.isEmpty() && manualInputArmed && graceOver) {
            cancel(active, "manual_input:" + manualReason);
            return;
        }
        if (manualReason.isEmpty() && graceOver) {
            manualInputArmed = true;
        }
    }

    private void tickActiveSession(ClientFollowState.ActiveTarget active, boolean keyDown) {
        if (!active.actorId().equals(activeActorId)) {
            activeActorId = active.actorId();
            activeStartedAtMillis = Util.getMillis();
            manualInputArmed = false;
        }
        tickActive(active, keyDown);
    }

    private void tickInactive(boolean keyDown) {
        activeActorId = "";
        activeStartedAtMillis = -1L;
        manualInputArmed = false;
        tickPrompt(keyDown);
    }

    private void tickPrompt(boolean keyDown) {
        long now = Util.getMillis();
        Minecraft minecraft = Minecraft.getInstance();
        String blockReason = ClientModeGuards.promptBlockReason(minecraft);
        int targetEntityId = FollowTargeting.targetedEntityId();
        if (!blockReason.isEmpty()) {
            lastProbeAtMillis = -1L;
            lastProbeTargetEntityId = -1;
            state.waitForProbe();
            state.resetHold();
            return;
        }
        if (targetEntityId >= 0 || keyDown) {
            requestPrompt(now, targetEntityId);
        }
        if (!keyDown) {
            state.resetHold();
            return;
        }
        state.prompt(now).ifPresentOrElse(prompt -> startIfTargeted(prompt, now), state::resetHold);
    }

    private void startIfTargeted(ClientFollowState.Prompt prompt, long now) {
        boolean targeted = FollowTargeting.canUsePrompt(prompt);
        if (targeted && !state.startRequested()) {
            state.beginHold(now);
            if (state.holdComplete(now)) {
                state.markStartRequested();
                FollowNetwork.sendToServer(new ServerboundFollowStartPacket(prompt.actorId()));
            }
        } else if (!targeted) {
            state.resetHold();
        }
    }

    private void cancel(ClientFollowState.ActiveTarget active, String reason) {
        state.stop();
        movement.release();
        activeActorId = "";
        activeStartedAtMillis = -1L;
        manualInputArmed = false;
        FollowNetwork.sendToServer(new ServerboundFollowCancelPacket(active.actorId(), reason));
    }

    private void requestPrompt(long now, int targetEntityId) {
        boolean sameTarget = targetEntityId == lastProbeTargetEntityId;
        if (sameTarget && lastProbeAtMillis >= 0L && now - lastProbeAtMillis < FollowConstants.PROBE_INTERVAL_MILLIS) {
            return;
        }
        lastProbeAtMillis = now;
        lastProbeTargetEntityId = targetEntityId;
        FollowNetwork.sendToServer(new ServerboundFollowProbePacket(targetEntityId));
    }
}
