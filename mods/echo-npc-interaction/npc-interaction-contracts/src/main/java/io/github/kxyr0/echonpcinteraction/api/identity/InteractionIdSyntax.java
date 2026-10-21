package io.github.kxyr0.echonpcinteraction.api.identity;

final class InteractionIdSyntax {
    private InteractionIdSyntax() {
    }

    static String requireNamespaced(String value, String type) {
        String checked = value == null ? "" : value.trim();
        if (checked.isEmpty() || checked.length() > 160 || !checked.contains(":")) {
            throw new IllegalArgumentException(type + " must be namespaced and at most 160 characters.");
        }
        return checked;
    }
}
