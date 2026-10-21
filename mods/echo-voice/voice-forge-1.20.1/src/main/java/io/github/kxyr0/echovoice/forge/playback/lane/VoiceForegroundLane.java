package io.github.kxyr0.echovoice.forge.playback.lane;

import io.github.kxyr0.echovoice.api.asset.VoiceAssetSnapshot;
import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnostic;
import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnosticCode;
import io.github.kxyr0.echovoice.api.identity.VoicePlaybackId;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackOutcome;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackReceipt;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackRequest;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackSnapshot;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackStatus;
import io.github.kxyr0.echovoice.api.playback.VoiceStopReason;
import io.github.kxyr0.echovoice.forge.api.VoicePlaybackView;
import io.github.kxyr0.echovoice.forge.cache.VoiceCacheLease;
import io.github.kxyr0.echovoice.forge.diagnostic.VoiceLogSink;
import io.github.kxyr0.echovoice.forge.playback.sound.VoiceSoundInstance;
import io.github.kxyr0.echovoice.forge.playback.sound.VoiceStopRamp;
import io.github.kxyr0.echovoice.forge.service.VoicePlaybackListeners;
import java.util.Optional;
import net.minecraft.client.Minecraft;

/** Client-thread owner of the only DIALOGUE_FOREGROUND_VOICE_LANE. */
public final class VoiceForegroundLane implements VoicePlaybackView {
    private final VoicePlaybackListeners listeners;
    private final VoiceLogSink diagnostics;
    private final VoiceStopRamp stopRamp;
    private VoicePendingStart pending;
    private VoiceActivePlayback active;
    private long clientTick;
    private long highestPresentationGeneration = -1;
    private long soundEngineGeneration;
    private long staleStartRejects;
    private VoicePlaybackReceipt lastReceipt;
    private VoiceDecodeMetrics decodeMetrics = VoiceDecodeMetrics.none();

    public VoiceForegroundLane(VoicePlaybackListeners listeners, VoiceLogSink diagnostics, VoiceStopRamp stopRamp) {
        this.listeners = listeners;
        this.diagnostics = diagnostics;
        this.stopRamp = stopRamp;
    }

    public VoicePlaybackReceipt request(VoicePlaybackRequest request, VoiceAssetSnapshot asset) {
        VoicePlaybackId id = VoicePlaybackId.next();
        if (request.presentationGeneration() < highestPresentationGeneration) {
            staleStartRejects++;
            return receipt(id, VoicePlaybackOutcome.REJECTED_STALE, Optional.empty(), 0, "stale presentation");
        }
        if (duplicate(request)) {
            return receipt(id, VoicePlaybackOutcome.REJECTED_DUPLICATE, Optional.empty(), 0, "duplicate presentation");
        }
        highestPresentationGeneration = request.presentationGeneration();
        clearPending();
        pending = new VoicePendingStart(id, request, asset, clientTick + 1);
        stopActive(VoiceStopReason.LINE_ADVANCED);
        return receipt(id, VoicePlaybackOutcome.QUEUED, Optional.empty(), 0, "decode queued");
    }

    public void ready(VoicePlaybackId id, VoiceCacheLease lease, long decodeLatencyMillis, boolean cacheHit) {
        if (pending == null || !pending.id.equals(id)) {
            lease.close();
            staleStartRejects++;
            diagnostics.accept(VoiceDiagnostic.warning(VoiceDiagnosticCode.VOICE_PLAYBACK_STALE_START_REJECTED,
                    "Decoded stale presentation was not started.", null));
            return;
        }
        pending.ready(lease, decodeLatencyMillis, cacheHit);
        decodeMetrics = new VoiceDecodeMetrics(pending.asset.format().name() + "VoiceDecoder", decodeLatencyMillis,
                cacheHit ? "HIT" : "MISS");
    }

    public VoicePlaybackReceipt stop(VoiceStopReason reason) {
        VoicePlaybackId id = active != null ? active.id : pending != null ? pending.id : VoicePlaybackId.next();
        clearPending();
        highestPresentationGeneration++;
        if (active == null) {
            return receipt(id, VoicePlaybackOutcome.INTERRUPTED, Optional.of(reason), 0, "lane already idle");
        }
        stopActive(reason);
        return receipt(id, VoicePlaybackOutcome.QUEUED, Optional.of(reason), active.playedMillis(), "stop queued");
    }

    public void fail(VoicePlaybackId id, VoicePlaybackOutcome outcome, String detail) {
        if (pending == null || !pending.id.equals(id)) {
            return;
        }
        pending.closeLease();
        pending = null;
        lastReceipt = receipt(id, outcome, Optional.empty(), 0, detail);
        listeners.publish(lastReceipt);
    }

    public void tick(Minecraft minecraft) {
        clientTick++;
        confirmTerminal(minecraft);
        if (active == null && pending != null && pending.lease != null && clientTick >= pending.eligibleTick) {
            startPending(minecraft);
        }
    }

    public void soundEngineReloaded() {
        soundEngineGeneration++;
        diagnostics.accept(VoiceDiagnostic.warning(VoiceDiagnosticCode.VOICE_SOUND_ENGINE_RESET,
                "Minecraft sound engine generation changed.", active == null ? null : active.asset.key()));
        if (active != null && active.stopReason == null) {
            VoicePendingStart restart = new VoicePendingStart(active.id, active.request, active.asset, clientTick + 1);
            restart.ready(active.lease, 0, true);
            active = null;
            clearPending();
            pending = restart;
        }
    }

    @Override
    public VoicePlaybackSnapshot snapshot() {
        if (active != null) {
            VoicePlaybackStatus status = active.stopReason == null
                    ? VoicePlaybackStatus.PLAYING : VoicePlaybackStatus.STOPPING;
            return new VoicePlaybackSnapshot(status, Optional.of(active.id), Optional.of(active.request),
                    Optional.of(active.asset), active.playedMillis(), soundEngineGeneration,
                    Optional.ofNullable(active.stopReason), 1, staleStartRejects);
        }
        if (pending != null) {
            VoicePlaybackStatus status = pending.lease == null
                    ? VoicePlaybackStatus.DECODING : VoicePlaybackStatus.READY;
            return new VoicePlaybackSnapshot(status, Optional.of(pending.id), Optional.of(pending.request),
                    Optional.of(pending.asset), 0, soundEngineGeneration, Optional.empty(), 0, staleStartRejects);
        }
        return VoicePlaybackSnapshot.idle(soundEngineGeneration, staleStartRejects);
    }

    public VoiceDecodeMetrics decodeMetrics() {
        return decodeMetrics;
    }

    private void startPending(Minecraft minecraft) {
        VoiceSoundInstance sound = new VoiceSoundInstance(pending.id, pending.lease.pcm(), pending.request.volume());
        try {
            minecraft.getSoundManager().play(sound);
            active = new VoiceActivePlayback(pending, sound, soundEngineGeneration);
            lastReceipt = receipt(active.id, VoicePlaybackOutcome.STARTED, Optional.empty(), 0, "playback started");
            listeners.publish(lastReceipt);
            pending = null;
        } catch (RuntimeException exception) {
            pending.closeLease();
            lastReceipt = receipt(pending.id, VoicePlaybackOutcome.FAILED_AUDIO_DEVICE, Optional.empty(), 0,
                    exception.getClass().getSimpleName());
            listeners.publish(lastReceipt);
            pending = null;
        }
    }

    private void confirmTerminal(Minecraft minecraft) {
        if (active == null || minecraft.getSoundManager().isActive(active.sound)) {
            return;
        }
        if (active.stopReason == null && System.currentTimeMillis() - active.startedAtMillis < 100) {
            return;
        }
        VoicePlaybackOutcome outcome = active.stopReason == null
                ? VoicePlaybackOutcome.COMPLETED : VoicePlaybackOutcome.INTERRUPTED;
        lastReceipt = receipt(active.id, outcome, Optional.ofNullable(active.stopReason), active.playedMillis(),
                outcome == VoicePlaybackOutcome.COMPLETED ? "playback completed" : "stop confirmed");
        stopRamp.release(active.sound);
        active.lease.close();
        active = null;
        listeners.publish(lastReceipt);
    }

    private void stopActive(VoiceStopReason reason) {
        if (active != null && active.stopReason == null) {
            active.stopReason = reason;
            stopRamp.stop(active.sound,
                    io.github.kxyr0.echovoice.forge.config.EchoVoiceClientConfig.STOP_RAMP_MS.get());
        }
    }

    private boolean duplicate(VoicePlaybackRequest request) {
        return active != null && active.request.equals(request) || pending != null && pending.request.equals(request);
    }

    private void clearPending() {
        if (pending != null) {
            pending.closeLease();
            pending = null;
        }
    }

    private VoicePlaybackReceipt receipt(VoicePlaybackId id, VoicePlaybackOutcome outcome,
            Optional<VoiceStopReason> reason, long playedMillis, String detail) {
        return new VoicePlaybackReceipt(id, outcome, reason, playedMillis, detail);
    }
}
