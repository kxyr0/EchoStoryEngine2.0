package io.github.kxyr0.echonavigation.api.identity;

import java.util.Objects;

/**
 * Shared durable ID syntax for Echo Navigation contracts.
 */
final class NavigationIdSyntax {
    private NavigationIdSyntax() {
    }

    static String requireNamespaced(String value, String typeName) {
        String checked = requireToken(value, typeName);
        if (!checked.contains(":")) {
            throw new IllegalArgumentException(typeName + " must be namespaced.");
        }
        return checked;
    }

    static String requireToken(String value, String typeName) {
        String checked = Objects.requireNonNull(value, typeName).trim();
        if (checked.isEmpty() || checked.length() > 160 || checked.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(typeName + " must be non-empty token up to 160 chars.");
        }
        return checked;
    }
}
