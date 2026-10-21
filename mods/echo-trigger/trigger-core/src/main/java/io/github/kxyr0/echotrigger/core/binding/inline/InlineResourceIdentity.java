package io.github.kxyr0.echotrigger.core.binding.inline;

import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassCallSyntax;
import io.github.kxyr0.echotrigger.core.binding.TriggerCallSyntax;
import java.util.Arrays;
import java.util.Locale;

final class InlineResourceIdentity {
    private InlineResourceIdentity() { }

    static String resolve(String sourceId, ClassSyntax resource) {
        var ids = resource.members().stream().filter(ClassCallSyntax.class::isInstance)
                .map(ClassCallSyntax.class::cast).map(ClassCallSyntax::invocation)
                .filter(call -> TriggerCallSyntax.qualifiedName(call).equals("Id")).toList();
        if (ids.size() > 1) {
            throw new IllegalArgumentException("Ресурс допускает один Id");
        }
        if (!ids.isEmpty()) {
            return TriggerCallSyntax.positionalString(ids.get(0), 0)
                    .orElseThrow(() -> new IllegalArgumentException("Id требует строковый ResourceId"));
        }
        String path = sourceId.replace('\\', '/');
        String[] parts = path.split("/");
        String namespace = namespace(parts, path);
        String file = parts[parts.length - 1];
        String stem = file.split("\\.", 2)[0];
        return namespace + ":" + stem.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
    }

    private static String namespace(String[] parts, String path) {
        for (int i = 0; i + 1 < parts.length; i++) {
            if (parts[i].equals("data")) {
                return parts[i + 1];
            }
        }
        for (int i = 0; i + 1 < parts.length; i++) {
            if (parts[i].equals("content-pack")) {
                return parts[i + 1];
            }
        }
        int colon = path.indexOf(':');
        if (colon > 0 && path.substring(0, colon).matches("[a-z0-9_.-]+")) {
            return path.substring(0, colon);
        }
        throw new IllegalArgumentException("Для inline When требуется namespaced source ID: "
                + Arrays.toString(parts));
    }
}
