package io.github.kxyr0.echodialogue.echoscript.resource;

import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import java.util.Objects;

/**
 * Typed content ownership supplied to semantic resource binders.
 */
public record SourceResourceContext(
        String sourceId,
        String contentPackId,
        String resourceNamespace,
        String resourceKind,
        String logicalFolder,
        String fileBasename
) {
    public SourceResourceContext {
        sourceId = required(sourceId, "sourceId");
        contentPackId = required(contentPackId, "contentPackId");
        resourceNamespace = required(resourceNamespace, "resourceNamespace");
        resourceKind = required(resourceKind, "resourceKind");
        logicalFolder = Objects.requireNonNull(logicalFolder, "logicalFolder");
        fileBasename = required(fileBasename, "fileBasename");
    }

    public DialogueId inferredDialogueId() {
        return DialogueId.parse(resourceNamespace + ":" + fileBasename);
    }

    private static String required(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return checked;
    }
}
