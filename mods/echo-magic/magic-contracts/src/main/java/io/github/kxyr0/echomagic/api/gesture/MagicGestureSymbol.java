package io.github.kxyr0.echomagic.api.gesture;

import io.github.kxyr0.echomagic.api.identity.MagicSchoolId;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum MagicGestureSymbol {
    FIRE("fire"),
    WATER("water"),
    EARTH("earth"),
    AIR("air"),
    BLOOD("blood"),
    VOID("void");

    private final String serializedName;

    MagicGestureSymbol(String serializedName) { this.serializedName = serializedName; }

    public String serializedName() { return serializedName; }

    public String translationKey() { return "gesture.echo_magic." + serializedName; }

    public boolean matches(MagicSchoolId school) {
        String value = school.value();
        int separator = value.indexOf(':');
        String path = separator < 0 ? value : value.substring(separator + 1);
        return serializedName.equals(path);
    }

    public static Optional<MagicGestureSymbol> fromSerialized(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(symbol -> symbol.serializedName.equals(normalized)).findFirst();
    }
}
