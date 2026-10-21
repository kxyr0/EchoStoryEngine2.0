package io.github.kxyr0.echoobservation.echoscript.binding;

import io.github.kxyr0.echoobservation.api.identity.ObservationResourceId;
import java.util.Optional;
import java.util.regex.Pattern;

record ObservationSourceContext(String sourceId, ObservationResourceId inferredId) {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");

    static Optional<ObservationSourceContext> resolve(String sourceId) {
        String normalized = sourceId.replace('\\', '/');
        int colon = normalized.indexOf(':');
        if (colon < 1 || !NAMESPACE.matcher(normalized.substring(0, colon)).matches()) {
            return Optional.empty();
        }
        String namespace = normalized.substring(0, colon);
        String file = normalized.substring(normalized.lastIndexOf('/') + 1);
        String basename = file.endsWith(".observation.es")
                ? file.substring(0, file.length() - ".observation.es".length())
                : file.endsWith(".es") ? file.substring(0, file.length() - 3) : "";
        if (basename.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new ObservationSourceContext(sourceId,
                new ObservationResourceId(namespace, normalize(basename))));
    }

    private static String normalize(String basename) {
        return basename.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
    }
}
