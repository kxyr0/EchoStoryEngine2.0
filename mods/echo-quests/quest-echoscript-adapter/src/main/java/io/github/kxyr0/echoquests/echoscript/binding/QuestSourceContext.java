package io.github.kxyr0.echoquests.echoscript.binding;

import io.github.kxyr0.echoquests.api.identity.QuestId;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public record QuestSourceContext(String sourceId, QuestId inferredId) {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final String ROOT = "echo_quests/quests/";

    public static Optional<QuestSourceContext> resolve(String sourceId) {
        String normalized = sourceId.replace('\\', '/').toLowerCase(Locale.ROOT);
        String namespace = namespace(normalized);
        int root = normalized.indexOf(ROOT);
        if (namespace == null || root < 0) return Optional.empty();
        String path = normalized.substring(root + ROOT.length());
        path = strip(path);
        if (path.isBlank()) return Optional.empty();
        return Optional.of(new QuestSourceContext(sourceId, QuestId.parse(namespace + ":" + path)));
    }

    private static String namespace(String source) {
        int colon = source.indexOf(':');
        if (colon > 0 && NAMESPACE.matcher(source.substring(0, colon)).matches()) return source.substring(0, colon);
        String[] parts = source.split("/");
        for (int index = 0; index + 1 < parts.length; index++) {
            if (parts[index].equals("data") && NAMESPACE.matcher(parts[index + 1]).matches()) return parts[index + 1];
        }
        return null;
    }

    private static String strip(String path) {
        if (path.endsWith(".quest.es")) return path.substring(0, path.length() - ".quest.es".length());
        if (path.endsWith(".es")) return path.substring(0, path.length() - ".es".length());
        return "";
    }
}
