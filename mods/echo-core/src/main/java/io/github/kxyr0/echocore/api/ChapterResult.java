package io.github.kxyr0.echocore.api;

public record ChapterResult(String chapter, Code code, String detail) {
    public enum Code {
        NONE, NO_SAVE, UNKNOWN_CHAPTER, INVALID_SAVE, TEMPLATE_MISSING, TEMPLATE_INVALID,
        SAVE_LOCKED, TEMPLATE_LOCKED, FOREIGN_SAVE, BUSY, WORLD_OPEN, IO_ERROR, CREATED, LOAD_REQUESTED,
        ACTIVE, LOAD_CANCELLED
    }
    public boolean success() { return code == Code.CREATED || code == Code.LOAD_REQUESTED || code == Code.ACTIVE; }
}
