package io.github.kxyr0.echostoryengine.blackspace.client;

import io.github.kxyr0.echostoryengine.blackspace.shader.BlackspaceShaderController;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BlackspaceClientState {
    private static final AtomicBoolean ACTIVE = new AtomicBoolean();
    private static volatile String blackspaceId = "";

    private BlackspaceClientState() {
    }

    public static boolean isActive() {
        return ACTIVE.get();
    }

    public static String blackspaceId() {
        return blackspaceId;
    }

    public static void applySessionState(boolean active, String nextBlackspaceId, long resetId) {
        if (active) {
            activate(nextBlackspaceId);
        } else {
            clear(resetId);
        }
    }

    public static void setActive(boolean active) {
        if (active) {
            activate("");
        } else {
            clear();
        }
    }

    public static void clear() {
        clear(0L);
    }

    public static void clear(long resetId) {
        blackspaceId = "";
        if (ACTIVE.compareAndSet(true, false)) {
            BlackspaceShaderController.requestExit(resetId);
        } else {
            BlackspaceShaderController.requestExit(resetId);
        }
    }

    public static void clearImmediately() {
        ACTIVE.set(false);
        blackspaceId = "";
        BlackspaceShaderController.abortAndRestore();
    }

    private static void activate(String nextBlackspaceId) {
        blackspaceId = nextBlackspaceId == null ? "" : nextBlackspaceId;
        if (ACTIVE.compareAndSet(false, true)) {
            BlackspaceShaderController.markEntered();
        }
    }
}
