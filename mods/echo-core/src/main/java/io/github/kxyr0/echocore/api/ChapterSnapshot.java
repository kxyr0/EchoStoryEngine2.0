package io.github.kxyr0.echocore.api;

public record ChapterSnapshot(String chapter, State state, boolean saveExists, boolean templateExists,
        String saveId, ChapterResult lastResult) {
    public enum State { NO_SAVE, READY, ACTIVE, BUSY, INVALID_SAVE, UNKNOWN_CHAPTER }
}
