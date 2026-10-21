package io.github.kxyr0.echodialogue.forge.client;

import io.github.kxyr0.echouianimations.AnimationChannel;
import io.github.kxyr0.echouianimations.AnimationEasing;
import io.github.kxyr0.echouianimations.AnimationFrame;
import io.github.kxyr0.echouianimations.AnimationPreset;
import io.github.kxyr0.echouianimations.AnimationPresetCatalog;
import io.github.kxyr0.echouianimations.AnimationTimeline;
import io.github.kxyr0.echodialogue.forge.network.message.DialogueOpenMessage;
import java.util.Optional;

/**
 * Client-side animation lifecycle for the current dialogue presentation.
 */
final class ClientDialogueAnimationState {
    private static final AnimationChannel FULL_CHANNEL = AnimationChannel.fixed(1.0F);

    private DialogueOpenMessage message;
    private boolean closing;
    private long openedAtMs;
    private long textStartedAtMs;
    private long closeStartedAtMs;
    private long resizeStartedAtMs;
    private int fromWidth;
    private int fromHeight;
    private int toWidth;
    private int toHeight;
    private int closingVisibleCharacters;

    void open(DialogueOpenMessage next, long nowMs) {
        ClientDialogueOverlayStyle.refresh();
        PanelSize currentSize = message == null ? new PanelSize(0, 0) : currentPanelSize(nowMs);
        boolean first = message == null || closing || !message.sessionId().equals(next.sessionId());
        boolean changed = first || message.revision() != next.revision() || message.kind() != next.kind();
        if (first) {
            openedAtMs = nowMs;
            fromWidth = 0;
            fromHeight = 0;
            toWidth = 0;
            toHeight = 0;
        }
        if (changed) {
            textStartedAtMs = nowMs + ClientDialogueOverlayStyle.current().textAppearDelayMs();
            fromWidth = currentSize.width();
            fromHeight = currentSize.height();
            toWidth = currentSize.width();
            toHeight = currentSize.height();
            resizeStartedAtMs = nowMs;
        }
        message = next;
        closing = false;
    }

    void close(long nowMs) {
        if (message != null && !closing) {
            closingVisibleCharacters = AnimationTimeline.sample(
                    typewriterPreset(), nowMs - textStartedAtMs, textLength()).visibleCharacters();
            closing = true;
            closeStartedAtMs = nowMs;
        }
    }

    boolean revealText(long nowMs) {
        if (message == null || closing || typewriterComplete(nowMs)) {
            return false;
        }
        textStartedAtMs = nowMs - typewriterDurationMs();
        return true;
    }

    boolean typewriterComplete(long nowMs) {
        return AnimationTimeline.sample(typewriterPreset(), nowMs - textStartedAtMs, textLength()).finished();
    }

    Optional<RenderState> renderState(long nowMs) {
        if (message == null) {
            return Optional.empty();
        }
        if (closing && closeFrame(nowMs).finished() && closeTextFrame(nowMs).finished()) {
            clear();
            return Optional.empty();
        }
        AnimationFrame panel = closing ? closeFrame(nowMs)
                : AnimationTimeline.sample(AnimationPresetCatalog.PANEL_OPEN, nowMs - openedAtMs, textLength());
        AnimationFrame text = closing ? fullTextFrame()
                : AnimationTimeline.sample(typewriterPreset(), nowMs - textStartedAtMs, textLength());
        AnimationFrame textFade = closing ? closeTextFrame(nowMs)
                : AnimationTimeline.sample(AnimationPresetCatalog.FADE_IN, nowMs - textStartedAtMs, textLength());
        return Optional.of(new RenderState(message, panel, text, textFade, closing));
    }

    PanelSize panelSize(int targetWidth, int targetHeight, long nowMs) {
        if (toWidth == 0 || toHeight == 0) {
            fromWidth = targetWidth;
            fromHeight = targetHeight;
            toWidth = targetWidth;
            toHeight = targetHeight;
        } else if (!closing && (targetWidth != toWidth || targetHeight != toHeight)) {
            PanelSize current = currentPanelSize(nowMs);
            fromWidth = current.width();
            fromHeight = current.height();
            toWidth = targetWidth;
            toHeight = targetHeight;
            resizeStartedAtMs = nowMs;
        }
        return currentPanelSize(nowMs);
    }

    void clear() {
        message = null;
        closing = false;
        fromWidth = 0;
        fromHeight = 0;
        toWidth = 0;
        toHeight = 0;
        closingVisibleCharacters = 0;
    }

    private PanelSize currentPanelSize(long nowMs) {
        AnimationFrame frame = AnimationTimeline.sample(
                AnimationPresetCatalog.PANEL_RESIZE, nowMs - resizeStartedAtMs, textLength());
        int width = interpolate(fromWidth, toWidth, frame.widthFactor());
        int height = interpolate(fromHeight, toHeight, frame.heightFactor());
        return new PanelSize(width, height);
    }

    private AnimationFrame closeFrame(long nowMs) {
        return AnimationTimeline.sample(AnimationPresetCatalog.PANEL_CLOSE, nowMs - closeStartedAtMs, textLength());
    }

    private AnimationFrame closeTextFrame(long nowMs) {
        return AnimationTimeline.sample(AnimationPresetCatalog.FADE_OUT, nowMs - closeStartedAtMs, textLength());
    }

    private AnimationFrame fullTextFrame() {
        int visibleCharacters = closing ? closingVisibleCharacters : textLength();
        return new AnimationFrame(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, visibleCharacters, true);
    }

    private AnimationPreset typewriterPreset() {
        return new AnimationPreset("dialogue_typewriter", typewriterDurationMs(), 0, AnimationEasing.LINEAR,
                FULL_CHANNEL, FULL_CHANNEL, FULL_CHANNEL, FULL_CHANNEL, true);
    }

    private int typewriterDurationMs() {
        int calculated = textLength() * ClientDialogueOverlayStyle.current().typewriterMsPerCharacter();
        return clamp(calculated, ClientDialogueOverlayStyle.current().typewriterMinDurationMs(),
                ClientDialogueOverlayStyle.current().typewriterMaxDurationMs());
    }

    private int textLength() {
        return message == null ? 0 : message.text().length();
    }

    private static int interpolate(int from, int to, float progress) {
        return Math.round(from + (to - from) * progress);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    record RenderState(
            DialogueOpenMessage message,
            AnimationFrame panel,
            AnimationFrame text,
            AnimationFrame textFade,
            boolean closing
    ) {
    }

    record PanelSize(int width, int height) {
    }
}
