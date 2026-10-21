package io.github.kxyr0.echocontentdoctor.source;

import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import java.util.Locale;

public final class SourceIdentities {
    private SourceIdentities() { }

    public static ContentResourceId inferred(String sourceId) {
        String normalized = sourceId.replace('\\', '/');
        String[] parts = normalized.split("/");
        String namespace = "unknown";
        for (int index = 0; index + 1 < parts.length; index++) {
            if (parts[index].equals("data")) namespace = parts[index + 1];
        }
        String filename = parts[parts.length - 1].toLowerCase(Locale.ROOT);
        int dot = filename.indexOf('.');
        String path = dot < 0 ? filename : filename.substring(0, dot);
        path = path.replaceAll("[^a-z0-9_./-]", "_");
        return new ContentResourceId(namespace + ":" + path);
    }
}
