package io.github.kxyr0.echoplayer.core.template;

import io.github.kxyr0.echoplayer.api.result.PlayerProfileDiagnostic;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TemplateArgumentReader {
    private static final List<String> ALLOWED = List.of(
            "id", "nameMode", "displayName", "spark", "maxSpark", "skinMode", "skinId", "skinModel"
    );

    Map<String, ArgumentSyntax> read(List<ArgumentSyntax> arguments, String source,
                                     List<PlayerProfileDiagnostic> diagnostics) {
        Map<String, ArgumentSyntax> named = new LinkedHashMap<>();
        for (ArgumentSyntax argument : arguments) {
            if (argument.name().isEmpty()) {
                diagnostics.add(error("PlayerProfile accepts named arguments only.", source));
                continue;
            }
            String name = argument.name().orElseThrow();
            if (named.putIfAbsent(name, argument) != null) {
                diagnostics.add(error("PlayerProfile argument is duplicated: " + name, source));
            }
            if (!ALLOWED.contains(name)) {
                diagnostics.add(error("PlayerProfile argument is not supported: " + name, source));
            }
        }
        return named;
    }

    private PlayerProfileDiagnostic error(String message, String source) {
        return PlayerProfileDiagnostic.error("PLAYER_TEMPLATE_BIND_ERROR", message, source);
    }
}
