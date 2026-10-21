package io.github.kxyr0.echobackgroundnpc.echoscript.binding;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** File-owned resource identity following ES2 data-pack conventions. */
public record BackgroundResourceContext(String sourceId, String namespace, String basename) {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");

    public BackgroundResourceContext {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        namespace = Objects.requireNonNull(namespace, "namespace").toLowerCase(Locale.ROOT);
        basename = Objects.requireNonNull(basename, "basename").toLowerCase(Locale.ROOT);
    }
    public String resourceId() { return namespace + ":" + basename; }

    public static Optional<BackgroundResourceContext> resolve(String sourceId) {
        String normalized = sourceId.replace('\\', '/');
        String[] segments = normalized.split("/");
        String namespace = null;
        int colon = normalized.indexOf(':');
        if (colon > 0 && NAMESPACE.matcher(normalized.substring(0, colon)).matches()) {
            namespace = normalized.substring(0, colon);
        }
        for (int index = 0; namespace == null && index + 1 < segments.length; index++) {
            if (segments[index].equals("data") && NAMESPACE.matcher(segments[index + 1]).matches()) {
                namespace = segments[index + 1];
            }
        }
        if (namespace == null && segments.length > 1 && NAMESPACE.matcher(segments[0]).matches()) {
            namespace = segments[0];
        }
        String file = segments[segments.length - 1];
        String basename = strip(file);
        return namespace == null || basename.isBlank() ? Optional.empty()
                : Optional.of(new BackgroundResourceContext(sourceId, namespace, basename));
    }

    private static String strip(String file) {
        for (String suffix : new String[]{".background_npc.es", ".profile.es", ".appearance_pool.es",
                ".ambient_dialogue.es", ".es"}) {
            if (file.endsWith(suffix)) return file.substring(0, file.length() - suffix.length());
        }
        return "";
    }
}
