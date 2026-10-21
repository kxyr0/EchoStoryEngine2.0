package io.github.kxyr0.echoaudio.api;

public final class AudioFailure extends IllegalArgumentException {
    private final String code;
    public AudioFailure(String code, String message) { super(code + ": " + message); this.code = code; }
    public String code() { return code; }
}
