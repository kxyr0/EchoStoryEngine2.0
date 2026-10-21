package io.github.kxyr0.echoaudio.api;

public record AudioResourceId(String value) {
    public AudioResourceId {
        if (value == null || !value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
                || value.contains("..") || value.endsWith("/") || value.matches(".*\\.(wav|mp3|ogg)$")) {
            throw new AudioFailure("AUDIO_ASSET_NOT_FOUND", "Invalid extension-free sound ID: " + value);
        }
    }
}
