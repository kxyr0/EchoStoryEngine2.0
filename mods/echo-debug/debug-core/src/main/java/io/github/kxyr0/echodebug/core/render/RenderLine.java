package io.github.kxyr0.echodebug.core.render;

import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import java.util.Objects;

public record RenderLine(Vector3 from, Vector3 to, String role) {
    public RenderLine {
        from = Objects.requireNonNull(from, "from");
        to = Objects.requireNonNull(to, "to");
        role = Objects.requireNonNull(role, "role").trim();
        if (role.isEmpty()) {
            throw new IllegalArgumentException("Render line role is required.");
        }
    }
}
