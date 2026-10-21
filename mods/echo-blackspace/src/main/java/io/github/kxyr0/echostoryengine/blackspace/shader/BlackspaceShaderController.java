package io.github.kxyr0.echostoryengine.blackspace.shader;

import io.github.kxyr0.echostoryengine.blackspace.EchoBlackspace;
import io.github.kxyr0.echostoryengine.blackspace.network.BlackspaceNetwork;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EchoBlackspace.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BlackspaceShaderController {
    private static final long FADE_MILLIS = 350L;
    private static final long PIPELINE_TIMEOUT_MILLIS = 5_000L;
    private static Phase phase = Phase.IDLE;
    private static long phaseStartedAt;
    private static long prepareId;
    private static long pendingResetId;
    private static float fadeStartAlpha;
    private static float fadeTargetAlpha;
    private static long fadeStartedAt;
    private static long fadeDurationMillis;
    private static boolean blackspaceShaderActive;

    private BlackspaceShaderController() {
    }

    public static void prepareEnter(long nextPrepareId) {
        prepareId = nextPrepareId;
        pendingResetId = 0L;
        setPhase(Phase.ENTER_FADE_IN);
        fadeTo(1.0F, FADE_MILLIS);
    }

    public static void markEntered() {
        if (phase == Phase.ENTER_WAIT_PIPELINE || phase == Phase.ENTER_READY_SENT || phase == Phase.ENTER_FADE_IN) {
            setPhase(Phase.ACTIVE_FADE_OUT);
            fadeTo(0.0F, FADE_MILLIS);
            return;
        }
        setPhase(Phase.ACTIVE);
        fadeTo(0.0F, FADE_MILLIS);
    }

    public static void stop() {
        requestExit(0L);
    }

    public static void requestExit(long resetId) {
        if (resetId != 0L) {
            pendingResetId = resetId;
        }
        if (phase == Phase.IDLE && !blackspaceShaderActive) {
            recoverAbandonedSessionIfNeeded();
            acknowledgeResetIfNeeded();
            return;
        }
        setPhase(Phase.EXIT_FADE_IN);
        fadeTo(1.0F, FADE_MILLIS);
    }

    public static void abortAndRestore() {
        try {
            if (OculusShaderBridge.isOculusLoaded()) {
                OculusShaderBridge.restoreImmediately();
            }
        } catch (LinkageError | RuntimeException exception) {
            EchoBlackspace.LOGGER.warn("Failed to immediately restore Blackspace shader state.", exception);
        }
        blackspaceShaderActive = false;
        prepareId = 0L;
        pendingResetId = 0L;
        fadeStartAlpha = 0.0F;
        fadeTargetAlpha = 0.0F;
        fadeStartedAt = 0L;
        fadeDurationMillis = 0L;
        setPhase(Phase.IDLE);
    }

    public static void recoverAbandonedSessionIfNeeded() {
        try {
            if (OculusShaderBridge.isOculusLoaded()) {
                OculusShaderBridge.recoverMarkedShaderIfNeeded();
            }
        } catch (LinkageError | RuntimeException exception) {
            EchoBlackspace.LOGGER.warn("Failed to recover abandoned Blackspace shader marker.", exception);
        }
    }

    public static boolean useForgeVisualFallback() {
        return !blackspaceShaderActive;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        tick(Util.getMillis());
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        float alpha = fadeAlpha(Util.getMillis());
        if (alpha <= 0.0F) {
            return;
        }
        int alphaByte = Math.min(255, Math.max(0, Math.round(alpha * 255.0F)));
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.fill(0, 0, event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(), alphaByte << 24);
    }

    private static void tick(long now) {
        switch (phase) {
            case ENTER_FADE_IN -> {
                if (fadeComplete(now)) {
                    installAndSwitch();
                    setPhase(Phase.ENTER_WAIT_PIPELINE);
                }
            }
            case ENTER_WAIT_PIPELINE -> {
                if (pipelineReady() || now - phaseStartedAt >= PIPELINE_TIMEOUT_MILLIS) {
                    if (!pipelineReady()) {
                        blackspaceShaderActive = false;
                        EchoBlackspace.LOGGER.warn("Blackspace shader pipeline was not ready in time; using fallback.");
                    }
                    BlackspaceNetwork.sendShaderReady(prepareId);
                    setPhase(Phase.ENTER_READY_SENT);
                }
            }
            case ACTIVE_FADE_OUT -> {
                if (fadeComplete(now)) {
                    setPhase(Phase.ACTIVE);
                }
            }
            case EXIT_FADE_IN -> {
                if (fadeComplete(now)) {
                    restorePreviousShader();
                    setPhase(Phase.EXIT_FADE_OUT);
                    fadeTo(0.0F, FADE_MILLIS);
                }
            }
            case EXIT_FADE_OUT -> {
                if (fadeComplete(now)) {
                    prepareId = 0L;
                    setPhase(Phase.IDLE);
                    acknowledgeResetIfNeeded();
                }
            }
            default -> {
            }
        }
    }

    private static void installAndSwitch() {
        blackspaceShaderActive = false;
        try {
            if (!OculusShaderBridge.isOculusLoaded()) {
                return;
            }
            if (BlackspaceShaderInstaller.installBundledShaderpack().isEmpty()) {
                return;
            }
            blackspaceShaderActive = OculusShaderBridge.enterBlackspace();
        } catch (LinkageError | RuntimeException exception) {
            EchoBlackspace.LOGGER.warn("Failed to enable Blackspace shaderpack; using Forge fallback.", exception);
            blackspaceShaderActive = false;
        }
    }

    private static boolean pipelineReady() {
        if (!blackspaceShaderActive) {
            return true;
        }
        try {
            return OculusShaderBridge.isBlackspacePipelineReady();
        } catch (LinkageError | RuntimeException exception) {
            EchoBlackspace.LOGGER.warn("Failed while checking Blackspace shader pipeline readiness.", exception);
            blackspaceShaderActive = false;
            return true;
        }
    }

    private static void restorePreviousShader() {
        try {
            if (OculusShaderBridge.isOculusLoaded()) {
                OculusShaderBridge.restorePrevious();
            }
        } catch (LinkageError | RuntimeException exception) {
            EchoBlackspace.LOGGER.warn("Failed to restore previous shaderpack after Blackspace.", exception);
        } finally {
            blackspaceShaderActive = false;
        }
    }

    private static void acknowledgeResetIfNeeded() {
        if (pendingResetId != 0L) {
            long resetId = pendingResetId;
            pendingResetId = 0L;
            BlackspaceNetwork.sendClientResetAck(resetId);
        }
    }

    private static void fadeTo(float targetAlpha, long durationMillis) {
        long now = Util.getMillis();
        fadeStartAlpha = fadeAlpha(now);
        fadeTargetAlpha = clamp(targetAlpha);
        fadeStartedAt = now;
        fadeDurationMillis = Math.max(0L, durationMillis);
    }

    private static float fadeAlpha(long now) {
        if (fadeDurationMillis <= 0L) {
            return fadeTargetAlpha;
        }
        float progress = Math.min(1.0F, Math.max(0.0F,
                (float) (now - fadeStartedAt) / (float) fadeDurationMillis));
        float smoothed = progress * progress * (3.0F - 2.0F * progress);
        return fadeStartAlpha + (fadeTargetAlpha - fadeStartAlpha) * smoothed;
    }

    private static boolean fadeComplete(long now) {
        return fadeDurationMillis <= 0L || now - fadeStartedAt >= fadeDurationMillis;
    }

    private static void setPhase(Phase nextPhase) {
        phase = nextPhase;
        phaseStartedAt = Util.getMillis();
    }

    private static float clamp(float value) {
        return Math.min(1.0F, Math.max(0.0F, value));
    }

    private enum Phase {
        IDLE,
        ENTER_FADE_IN,
        ENTER_WAIT_PIPELINE,
        ENTER_READY_SENT,
        ACTIVE_FADE_OUT,
        ACTIVE,
        EXIT_FADE_IN,
        EXIT_FADE_OUT
    }
}
