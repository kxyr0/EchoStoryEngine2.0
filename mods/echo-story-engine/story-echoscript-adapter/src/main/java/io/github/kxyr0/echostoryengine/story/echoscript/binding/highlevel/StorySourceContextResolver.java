package io.github.kxyr0.echostoryengine.story.echoscript.binding.highlevel;

import java.util.Optional;
import java.util.regex.Pattern;

/** Resolves file-owned Story identity from canonical data-pack source IDs. */
final class StorySourceContextResolver {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");

    Optional<String> resolveStoryId(String sourceId) {
        String normalized = sourceId.replace('\\', '/');
        Optional<String> namespace = namespace(normalized);
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
        String basename = basename(fileName);
        if (namespace.isEmpty() || basename.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(namespace.orElseThrow() + ":" + basename);
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
        return Optional.empty();
    }

    private String basename(String fileName) {
        if (fileName.endsWith(".story.es")) {
            return fileName.substring(0, fileName.length() - ".story.es".length());
        }
        return fileName.endsWith(".es") ? fileName.substring(0, fileName.length() - 3) : "";
    }
}
