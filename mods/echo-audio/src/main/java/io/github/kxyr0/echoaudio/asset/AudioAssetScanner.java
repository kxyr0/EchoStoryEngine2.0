package io.github.kxyr0.echoaudio.asset;

import io.github.kxyr0.echoaudio.api.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

/** One authoritative scanner for client reload and Doctor; caller must run off the render thread. */
public final class AudioAssetScanner {
    public record Scan(List<AudioAssetDescriptor> assets, List<AudioFailure> problems, Map<AudioResourceId, AudioFailure> rejected) { }
    public Scan scan(Path contentPack) throws java.io.IOException {
        Map<AudioResourceId, List<Path>> candidates = new TreeMap<>(Comparator.comparing(AudioResourceId::value));
        List<AudioFailure> problems = new ArrayList<>();
        Map<AudioResourceId, AudioFailure> rejected = new LinkedHashMap<>();
        if (!Files.isDirectory(contentPack)) return new Scan(List.of(), List.of(), Map.of());
        try (var namespaces = Files.list(contentPack)) {
            for (Path namespace : namespaces.filter(Files::isDirectory).sorted().toList()) {
                Path sounds = namespace.resolve("sounds");
                if (!Files.isDirectory(sounds)) continue;
                try (var files = Files.walk(sounds)) {
                    for (Path path : files.filter(Files::isRegularFile).sorted().toList()) {
                        if (!path.toRealPath().startsWith(sounds.toRealPath())) continue;
                        String relative = sounds.relativize(path).toString().replace('\\', '/');
                        int dot = relative.lastIndexOf('.');
                        if (dot < 0) continue;
                        String extension = relative.substring(dot + 1).toLowerCase(Locale.ROOT);
                        if (!Set.of("wav", "ogg", "mp3").contains(extension)) {
                            problems.add(new AudioFailure("AUDIO_FORMAT_UNSUPPORTED", path.toString())); continue;
                        }
                        try {
                            AudioResourceId id = new AudioResourceId(namespace.getFileName() + ":" + relative.substring(0, dot));
                            candidates.computeIfAbsent(id, key -> new ArrayList<>()).add(path);
                        } catch (AudioFailure failure) { problems.add(failure); }
                    }
                }
            }
        }
        List<AudioAssetDescriptor> assets = new ArrayList<>();
        for (var entry : candidates.entrySet()) {
            if (entry.getValue().size() != 1) {
                var failure = new AudioFailure("AUDIO_ASSET_AMBIGUOUS", entry.getKey().value() + " " + entry.getValue());
                problems.add(failure); rejected.put(entry.getKey(), failure);
                continue;
            }
            Path path = entry.getValue().get(0);
            String name = path.getFileName().toString();
            assets.add(new AudioAssetDescriptor(entry.getKey(), revision(path), path,
                    name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)));
        }
        return new Scan(List.copyOf(assets), List.copyOf(problems), Map.copyOf(rejected));
    }
    public static AudioRevision revision(Path path) throws java.io.IOException {
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192]; int count;
                while ((count = input.read(buffer)) >= 0) hash.update(buffer, 0, count);
            }
            return new AudioRevision(HexFormat.of().formatHex(hash.digest()));
        } catch (java.security.NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
    }
}
