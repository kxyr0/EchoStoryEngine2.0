package io.github.kxyr0.echovoice.forge.catalog;

import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echovoice.api.asset.VoiceAssetFormat;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import io.github.kxyr0.echovoice.api.identity.VoiceFileStem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Deterministic mapping between DialogueId and external content-pack voice folders. */
public final class VoiceAssetPathResolver {
    private final Path contentPacksRoot;

    public VoiceAssetPathResolver(Path gameDirectory) {
        contentPacksRoot = gameDirectory.toAbsolutePath().normalize()
                .resolve("story").resolve("content-pack");
    }

    public Path contentPacksRoot() {
        return contentPacksRoot;
    }

    public Path dialogueFolder(DialogueId dialogueId) {
        String dialoguePath = dialogueId.value().path();
        if (java.util.Arrays.stream(dialoguePath.split("/")).anyMatch(part -> part.equals(".") || part.equals(".."))) {
            throw new IllegalArgumentException("Dialogue path must not contain traversal segments.");
        }
        Path namespaceRoot = contentPacksRoot.resolve(dialogueId.value().namespace())
                .resolve("echo_voice").resolve("dialogue")
                .normalize();
        Path folder = namespaceRoot.resolve(dialoguePath.replace('/', java.io.File.separatorChar)).normalize();
        if (!folder.startsWith(namespaceRoot)) {
            throw new IllegalArgumentException("Dialogue path escapes its voice namespace.");
        }
        return folder;
    }

    public List<Path> supportedFiles(VoiceAssetKey key) {
        Path folder = dialogueFolder(key.dialogueId());
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (var files = Files.list(folder)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> stem(path).map(value -> value.equalsIgnoreCase(key.stem().value())).orElse(false))
                    .filter(path -> VoiceAssetFormat.fromFilename(path.getFileName().toString()).isPresent())
                    .sorted()
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    public Optional<VoiceAssetKey> keyForPath(Path candidate) {
        Path path = candidate.toAbsolutePath().normalize();
        if (!path.startsWith(contentPacksRoot)) {
            return Optional.empty();
        }
        Path relative = contentPacksRoot.relativize(path);
        if (relative.getNameCount() < 5 || !relative.getName(1).toString().equals("echo_voice")
                || !relative.getName(2).toString().equals("dialogue")) {
            return Optional.empty();
        }
        String filename = relative.getFileName().toString();
        if (VoiceAssetFormat.fromFilename(filename).isEmpty()) {
            return Optional.empty();
        }
        StringBuilder dialoguePath = new StringBuilder();
        for (int index = 3; index < relative.getNameCount() - 1; index++) {
            if (!dialoguePath.isEmpty()) {
                dialoguePath.append('/');
            }
            dialoguePath.append(relative.getName(index));
        }
        try {
            DialogueId id = DialogueId.parse(relative.getName(0) + ":" + dialoguePath);
            return stem(path).map(value -> VoiceAssetKey.of(id, value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Optional<String> stem(Path path) {
        String filename = path.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        if (dot <= 0) {
            return Optional.empty();
        }
        try {
            return Optional.of(VoiceFileStem.parse(filename.substring(0, dot).toLowerCase(Locale.ROOT)).value());
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
