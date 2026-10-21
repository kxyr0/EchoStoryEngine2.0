package io.github.kxyr0.echodialogue.echoscript.resource;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Resolves Dialogue content ownership from canonical resource source IDs.
 */
public final class SourceResourceContextResolver {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");

    public Optional<SourceResourceContext> resolveDialogue(String sourceId) {
        String normalized = sourceId.replace('\\', '/');
        Optional<String> namespace = namespace(normalized);
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
        String basename = basename(fileName);
        if (namespace.isEmpty() || basename.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new SourceResourceContext(sourceId, namespace.orElseThrow(), namespace.orElseThrow(),
                "dialogue", logicalFolder(normalized), basename));
    }

    private Optional<String> namespace(String sourceId) {
        int colon = sourceId.indexOf(':');
        if (colon > 0 && NAMESPACE.matcher(sourceId.substring(0, colon)).matches()) {
            return Optional.of(sourceId.substring(0, colon));
        }
        String[] segments = sourceId.split("/");
        for (int index = 0; index + 1 < segments.length; index++) {
            if (segments[index].equals("data") && NAMESPACE.matcher(segments[index + 1]).matches()) {
                return Optional.of(segments[index + 1]);
            }
        }
        return segments.length > 1 && NAMESPACE.matcher(segments[0]).matches()
                ? Optional.of(segments[0]) : Optional.empty();
    }

    private String basename(String fileName) {
        if (fileName.endsWith(".dialogue.es")) {
            return fileName.substring(0, fileName.length() - ".dialogue.es".length());
        }
        return fileName.endsWith(".es") ? fileName.substring(0, fileName.length() - 3) : "";
    }

    private String logicalFolder(String sourceId) {
        String path = sourceId.substring(Math.max(0, sourceId.indexOf(':') + 1));
        int fileSeparator = path.lastIndexOf('/');
        if (fileSeparator < 0) {
            return "";
        }
        String[] segments = path.substring(0, fileSeparator).split("/");
        int root = Arrays.asList(segments).indexOf("dialogues");
        return root < 0 ? "" : String.join("/", Arrays.copyOfRange(segments, root + 1, segments.length));
    }
}
