package io.github.kxyr0.echoscene.compile.binding;

import io.github.kxyr0.echoscene.api.identity.SceneLocalId;
import io.github.kxyr0.echoscene.api.identity.SceneResourceId;
import java.util.Locale;

final class SceneIdInference {
    SceneResourceId resource(String namespace, String sourcePath) {
        String file = sourcePath.replace('\\', '/');
        file = file.substring(file.lastIndexOf('/') + 1);
        file = file.replaceFirst("(?i)(\\.scene)?\\.es$", "");
        return new SceneResourceId(namespace + ":" + snake(file));
    }

    SceneLocalId local(String method) {
        return new SceneLocalId(snake(method));
    }

    private String snake(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("[^A-Za-z0-9_]+", "_").toLowerCase(Locale.ROOT);
    }
}
