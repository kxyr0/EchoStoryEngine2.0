package io.github.kxyr0.echonewui.forge.client;

import io.github.kxyr0.echonewui.core.layout.JournalBookLayoutResult;
import java.util.Optional;

public final class JournalBookPreviewState {
    private boolean visible;
    private long visibleRenderCalls;
    private long hiddenRenderCalls;
    private JournalBookLayoutResult lastLayout;

    public synchronized boolean show() {
        boolean changed = !visible;
        visible = true;
        return changed;
    }

    public synchronized boolean hide() {
        boolean changed = visible;
        visible = false;
        return changed;
    }

    public synchronized boolean toggle() {
        visible = !visible;
        return visible;
    }

    public synchronized boolean visible() {
        return visible;
    }

    public synchronized void recordVisibleRender(JournalBookLayoutResult layout) {
        visibleRenderCalls++;
        lastLayout = layout;
    }

    public synchronized void recordHiddenRender() {
        hiddenRenderCalls++;
    }

    public synchronized Optional<JournalBookLayoutResult> lastLayout() {
        return Optional.ofNullable(lastLayout);
    }

    public synchronized long visibleRenderCalls() {
        return visibleRenderCalls;
    }

    public synchronized long hiddenRenderCalls() {
        return hiddenRenderCalls;
    }
}
