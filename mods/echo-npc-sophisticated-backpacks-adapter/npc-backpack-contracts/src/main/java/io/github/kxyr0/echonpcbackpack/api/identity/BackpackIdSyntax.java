package io.github.kxyr0.echonpcbackpack.api.identity;

final class BackpackIdSyntax {
    private BackpackIdSyntax() {
    }

    static String namespaced(String value, String label) {
        String normalized = java.util.Objects.requireNonNull(value, label).trim();
        if (normalized.length() > 128 || !normalized.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException(label + " must be a lowercase namespaced id.");
        }
        return normalized;
    }
}
