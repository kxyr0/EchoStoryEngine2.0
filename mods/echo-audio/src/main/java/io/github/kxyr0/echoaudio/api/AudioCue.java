package io.github.kxyr0.echoaudio.api;

import java.util.Map;

/** Portable command payload shared by Scene, Story and external action adapters. */
public record AudioCue(String operation, String resource, String name, String location, AudioOptions options, AudioSource source) {
    public AudioCue(String operation, String resource, String name, String location, AudioOptions options) {
        this(operation, resource, name, location, options, AudioSource.api());
    }
    public static final java.util.Set<String> OPERATIONS = java.util.Set.of(
            "Play", "Loop", "PlayAt", "LoopAt", "PlayAndWait", "LoopAndWait", "Stop");
    public AudioCue {
        java.util.Objects.requireNonNull(options); java.util.Objects.requireNonNull(source);
        if (!OPERATIONS.contains(operation)) throw new AudioFailure("AUDIO_PLAYBACK_START_FAILED", operation);
        if (!operation.equals("Stop")) new AudioResourceId(resource);
        if (!name.isEmpty() && !name.matches("[A-Za-z_][A-Za-z0-9_]{0,63}")) {
            throw new AudioFailure("AUDIO_PLAYBACK_NOT_FOUND", "Invalid playback name: " + name);
        }
        if (operation.equals("Stop") && name.isEmpty()) {
            throw new AudioFailure("AUDIO_PLAYBACK_NOT_FOUND", "Stop requires an owned playback name");
        }
        if (operation.equals("LoopAndWait") && options.durationTicks() == 0) {
            throw new AudioFailure("AUDIO_DURATION_INVALID", "LoopAndWait requires duration");
        }
    }
    public boolean loop() { return operation.startsWith("Loop"); }
    public boolean waitForEnd() { return operation.endsWith("AndWait"); }
    public void requireCompletionSafe() {
        if (waitForEnd() || loop() && options.durationTicks() == 0) {
            throw new AudioFailure("AUDIO_DURATION_INVALID", "OnComplete accepts one-shots and finite nonblocking loops");
        }
    }
    public java.util.List<String> arguments() {
        Map<String, String> values = payload();
        return keys().stream().map(values::get).toList();
    }
    public static AudioCue fromArguments(java.util.List<String> values) {
        if (values.size() != keys().size()) throw new AudioFailure("AUDIO_PLAYBACK_START_FAILED", "Invalid payload size");
        Map<String, String> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < values.size(); i++) map.put(keys().get(i), values.get(i));
        return decode(map);
    }
    private static java.util.List<String> keys() {
        return java.util.List.of("operation", "resource", "name", "location", "volume", "pitch", "duration", "fadeIn", "fadeOut",
                "source", "line", "column");
    }
    public Map<String, String> payload() {
        Map<String, String> values = new java.util.LinkedHashMap<>(Map.of("operation", operation,
                "resource", resource, "name", name, "location", location,
                "volume", "" + options.volume(), "pitch", "" + options.pitch(),
                "duration", "" + options.durationTicks(), "fadeIn", "" + options.fadeInTicks(),
                "fadeOut", "" + options.fadeOutTicks()));
        values.put("source", source.file()); values.put("line", "" + source.line()); values.put("column", "" + source.column());
        return Map.copyOf(values);
    }
    public static AudioCue decode(Map<String, String> map) {
        return new AudioCue(map.get("operation"), map.getOrDefault("resource", ""), map.getOrDefault("name", ""),
                map.getOrDefault("location", ""), new AudioOptions(Double.parseDouble(map.get("volume")),
                Double.parseDouble(map.get("pitch")), Long.parseLong(map.get("duration")),
                Long.parseLong(map.get("fadeIn")), Long.parseLong(map.get("fadeOut"))),
                new AudioSource(map.getOrDefault("source", "<api>"), Integer.parseInt(map.getOrDefault("line", "1")),
                        Integer.parseInt(map.getOrDefault("column", "1"))));
    }
}
