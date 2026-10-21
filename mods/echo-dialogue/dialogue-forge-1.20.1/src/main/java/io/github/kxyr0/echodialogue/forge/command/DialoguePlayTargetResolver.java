package io.github.kxyr0.echodialogue.forge.command;

import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.forge.catalog.CompiledDialogueCatalogSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Resolves dev command dialogue targets by ID or loaded source file name.
 */
final class DialoguePlayTargetResolver {
    Resolution resolve(CompiledDialogueCatalogSnapshot catalog, String rawTarget) {
        Objects.requireNonNull(catalog, "catalog");
        String target = normalize(Objects.requireNonNull(rawTarget, "rawTarget"));
        if (target.isBlank()) {
            return Resolution.missing(rawTarget);
        }
        var sourceMatch = catalog.findBySourceId(target);
        if (sourceMatch.isPresent()) {
            return Resolution.resolved(sourceMatch.orElseThrow());
        }
        if (target.contains(":") && !looksLikeScript(target)) {
            try {
                DialogueId id = DialogueId.parse(target);
                if (catalog.find(id).isPresent()) {
                    return Resolution.resolved(id);
                }
            } catch (IllegalArgumentException ignored) {
                // Fall through to source-name resolution.
            }
        }
        List<DialogueId> matches = sourceNameMatches(catalog, target);
        if (matches.size() == 1) {
            return Resolution.resolved(matches.get(0));
        }
        if (matches.size() > 1) {
            return Resolution.ambiguous(target, matches);
        }
        return Resolution.missing(rawTarget);
    }

    List<String> suggestions(CompiledDialogueCatalogSnapshot catalog) {
        Set<String> suggestions = new LinkedHashSet<>(catalog.sortedIds());
        catalog.sortedSourceIds().forEach(sourceId -> {
            suggestions.add(sourceId);
            suggestions.add(fileName(sourceId));
            suggestions.add(shortScriptName(fileName(sourceId)));
        });
        return suggestions.stream().filter(value -> !value.isBlank()).toList();
    }

    private List<DialogueId> sourceNameMatches(CompiledDialogueCatalogSnapshot catalog, String target) {
        String targetPath = sourcePath(target);
        String normalizedTarget = shortScriptName(fileName(targetPath));
        List<DialogueId> matches = new ArrayList<>();
        catalog.sourceDialogueIds().forEach((sourceId, dialogueId) -> {
            String sourcePath = sourcePath(sourceId);
            if (sourcePath.equals(targetPath)
                    || sourcePath.endsWith("/" + targetPath)
                    || shortScriptName(fileName(sourcePath)).equals(normalizedTarget)) {
                matches.add(dialogueId);
            }
        });
        return matches.stream().distinct().toList();
    }

    private String normalize(String value) {
        return value.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
    }

    private boolean looksLikeScript(String value) {
        return value.endsWith(".es");
    }

    private String sourcePath(String sourceId) {
        int separator = sourceId.indexOf(':');
        return separator >= 0 ? sourceId.substring(separator + 1) : sourceId;
    }

    private String fileName(String path) {
        String sourcePath = sourcePath(path);
        int separator = sourcePath.lastIndexOf('/');
        return separator >= 0 ? sourcePath.substring(separator + 1) : sourcePath;
    }

    private String shortScriptName(String fileName) {
        String value = fileName;
        if (value.endsWith(".dialogue.es")) {
            return value.substring(0, value.length() - ".dialogue.es".length());
        }
        if (value.endsWith(".es")) {
            return value.substring(0, value.length() - ".es".length());
        }
        return value;
    }

    record Resolution(DialogueId dialogueId, String missingTarget, List<DialogueId> ambiguousMatches) {
        Resolution {
            ambiguousMatches = List.copyOf(Objects.requireNonNull(ambiguousMatches, "ambiguousMatches"));
        }

        static Resolution resolved(DialogueId dialogueId) {
            return new Resolution(Objects.requireNonNull(dialogueId, "dialogueId"), "", List.of());
        }

        static Resolution missing(String target) {
            return new Resolution(null, Objects.requireNonNull(target, "target"), List.of());
        }

        static Resolution ambiguous(String target, List<DialogueId> matches) {
            return new Resolution(null, Objects.requireNonNull(target, "target"), matches);
        }

        boolean resolved() {
            return dialogueId != null;
        }

        boolean ambiguous() {
            return !ambiguousMatches.isEmpty();
        }
    }
}
