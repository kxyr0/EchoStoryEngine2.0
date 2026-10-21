package io.github.kxyr0.echoeffects.api.definition;

import io.github.kxyr0.echoeffects.api.identity.EffectId;
import java.util.Objects;

public record EffectPresentation(String nameKey, String descriptionKey, String icon) {
    public EffectPresentation {
        nameKey = requireText(nameKey, "nameKey");
        descriptionKey = requireText(descriptionKey, "descriptionKey");
        icon = requireText(icon, "icon");
    }

    public static EffectPresentation fallback(EffectId id) {
        String path = id.value().replace(':', '.').replace('/', '.');
        return new EffectPresentation("effect." + path, "effect." + path + ".description",
                id.value().replace(":", ":textures/echo_effects/icons/") + ".png");
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name);
        if (checked.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return checked;
    }
}
