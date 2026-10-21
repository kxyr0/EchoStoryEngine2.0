package io.github.kxyr0.echocore.api;

public final class ChapterApi {
    private static volatile ChapterBackend backend;
    private ChapterApi() { }
    public static ChapterBackend get() {
        if (backend == null) throw new IllegalStateException("Echo Core client backend has not initialized.");
        return backend;
    }
    public static synchronized void install(ChapterBackend value) {
        if (backend != null) throw new IllegalStateException("Backend already initialized.");
        backend = java.util.Objects.requireNonNull(value);
    }
}
