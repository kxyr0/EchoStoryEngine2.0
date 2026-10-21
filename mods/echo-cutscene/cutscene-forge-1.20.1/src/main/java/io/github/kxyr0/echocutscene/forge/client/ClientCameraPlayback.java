package io.github.kxyr0.echocutscene.forge.client;

import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutscenePlaybackId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneEndBehavior;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneHudPolicy;
import io.github.kxyr0.echocutscene.core.clock.CameraPlaybackClock;
import io.github.kxyr0.echocutscene.core.clock.PlaybackTime;
import io.github.kxyr0.echocutscene.core.curve.CameraCurveCompiler;
import io.github.kxyr0.echocutscene.core.curve.CompiledCameraCurve;
import io.github.kxyr0.echocutscene.core.target.CameraSemanticPoseEvaluator;
import io.github.kxyr0.echocutscene.forge.api.client.CameraPlaybackClientService;
import io.github.kxyr0.echocutscene.forge.client.body.ClientLocalBodyLock;
import io.github.kxyr0.echocutscene.forge.client.target.ClientCameraTargetResolver;
import io.github.kxyr0.echocutscene.forge.client.target.ClientViewportAspect;
import java.util.OptionalDouble;
import java.util.Objects;
import net.minecraft.Util;

public final class ClientCameraPlayback implements CameraPlaybackClientService {
    private static final long STOP_FADE_TO_BLACK_MILLIS = 500L;
    private static final long STOP_FADE_FROM_BLACK_MILLIS = 500L;
    private final ClientCameraRig rig = new ClientCameraRig();
    private final CameraCurveCompiler compiler = new CameraCurveCompiler();
    private final CameraSemanticPoseEvaluator evaluator = new CameraSemanticPoseEvaluator();
    private final ClientCameraTargetResolver targets = new ClientCameraTargetResolver();
    private CutscenePlaybackId playbackId;
    private CameraPlaybackClock clock;
    private CameraTrackSnapshot track;
    private CompiledCameraCurve curve;
    private CameraPose currentPose;
    private CutsceneEndBehavior endBehavior = CutsceneEndBehavior.AUTO_STOP;
    private CutsceneHudPolicy hudPolicy = CutsceneHudPolicy.defaultPlayback();
    private StopFadePhase stopFadePhase = StopFadePhase.NONE;
    private ClientLocalBodyLock.Snapshot bodySnapshot;
    private long stopFadeStartedAtMillis;

    @Override
    public void begin(CutscenePlaybackId id, CameraTrackSnapshot track, long startedAtMillis, double speed) {
        begin(id, track, startedAtMillis, speed, CutsceneEndBehavior.AUTO_STOP);
    }

    @Override
    public void begin(
            CutscenePlaybackId id,
            CameraTrackSnapshot track,
            long startedAtMillis,
            double speed,
            CutsceneEndBehavior nextEndBehavior
    ) {
        stopFadePhase = StopFadePhase.NONE;
        stopFadeStartedAtMillis = 0L;
        playbackId = id;
        this.track = track;
        endBehavior = nextEndBehavior;
        hudPolicy = CutsceneHudPolicy.defaultPlayback();
        bodySnapshot = ClientLocalBodyLock.capture().orElse(null);
        curve = compiler.compile(track);
        clock = new CameraPlaybackClock(startedAtMillis, track.metadata().durationMillis(), speed);
        currentPose = evaluate(0L);
        rig.detach(currentPose);
    }

    public void hudPolicy(CutsceneHudPolicy nextHudPolicy) {
        hudPolicy = Objects.requireNonNull(nextHudPolicy, "nextHudPolicy");
    }

    @Override
    public void end(CutscenePlaybackId id) {
        if (playbackId == null || !playbackId.equals(id) || stopFadePhase != StopFadePhase.NONE) {
            return;
        }
        stopFadePhase = StopFadePhase.FADING_TO_BLACK;
        stopFadeStartedAtMillis = Util.getMillis();
    }

    public void tick() {
        long nowMillis = Util.getMillis();
        if (stopFadePhase == StopFadePhase.FADING_FROM_BLACK) {
            tickStopFade(nowMillis);
            return;
        }
        if (playbackId == null || clock == null || curve == null) {
            return;
        }
        enforceBodyLock();
        PlaybackTime time = clock.at(nowMillis);
        currentPose = evaluate(time.elapsedMillis());
        rig.apply(currentPose);
        enforceBodyLock();
        if (time.ended() && endBehavior == CutsceneEndBehavior.AUTO_STOP
                && stopFadePhase == StopFadePhase.NONE) {
            clearImmediately();
            return;
        }
        tickStopFade(nowMillis);
    }

    public boolean playing() {
        return playbackId != null || stopFadePhase != StopFadePhase.NONE;
    }

    public boolean hudSuppressed() {
        return playing() && hudPolicy.hideGameplayHud();
    }

    public boolean cinematic() {
        return playing() && hudPolicy.cinematic();
    }

    public void clear() {
        clearImmediately();
    }

    public float fadeAlpha() {
        if (stopFadePhase == StopFadePhase.FADING_TO_BLACK) {
            return (float) smooth(progress(STOP_FADE_TO_BLACK_MILLIS));
        }
        if (stopFadePhase == StopFadePhase.FADING_FROM_BLACK) {
            return (float) (1.0D - smooth(progress(STOP_FADE_FROM_BLACK_MILLIS)));
        }
        return 0.0F;
    }

    public OptionalDouble currentFov() {
        if (currentPose == null) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(currentPose.fieldOfView().degrees());
    }

    private void tickStopFade(long nowMillis) {
        if (stopFadePhase == StopFadePhase.FADING_TO_BLACK && progress(STOP_FADE_TO_BLACK_MILLIS) >= 1.0D) {
            attachToBodyBehindBlack();
            stopFadePhase = StopFadePhase.FADING_FROM_BLACK;
            stopFadeStartedAtMillis = nowMillis;
        } else if (stopFadePhase == StopFadePhase.FADING_FROM_BLACK
                && progress(STOP_FADE_FROM_BLACK_MILLIS) >= 1.0D) {
            stopFadePhase = StopFadePhase.NONE;
            stopFadeStartedAtMillis = 0L;
            playbackId = null;
        }
    }

    private double progress(long durationMillis) {
        if (durationMillis <= 0L) {
            return 1.0D;
        }
        return Math.min(1.0D, Math.max(0.0D, (double) (Util.getMillis() - stopFadeStartedAtMillis)
                / durationMillis));
    }

    private static double smooth(double progress) {
        return progress * progress * (3.0D - 2.0D * progress);
    }

    private void clearImmediately() {
        rig.attachToBody();
        playbackId = null;
        clearPlaybackState();
        stopFadePhase = StopFadePhase.NONE;
        stopFadeStartedAtMillis = 0L;
    }

    private void attachToBodyBehindBlack() {
        rig.attachToBody();
        clearPlaybackState();
    }

    private void clearPlaybackState() {
        playbackId = null;
        clock = null;
        track = null;
        curve = null;
        currentPose = null;
        bodySnapshot = null;
        endBehavior = CutsceneEndBehavior.AUTO_STOP;
        hudPolicy = CutsceneHudPolicy.defaultPlayback();
    }

    private void enforceBodyLock() {
        if (bodySnapshot != null) {
            ClientLocalBodyLock.apply(bodySnapshot);
        }
    }

    private CameraPose evaluate(long elapsedMillis) {
        evaluator.aspect(ClientViewportAspect.current());
        return evaluator.evaluate(curve, track, elapsedMillis, 0.0D, targets);
    }

    private enum StopFadePhase {
        NONE,
        FADING_TO_BLACK,
        FADING_FROM_BLACK
    }
}
