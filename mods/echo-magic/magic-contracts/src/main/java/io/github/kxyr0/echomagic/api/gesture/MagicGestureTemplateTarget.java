package io.github.kxyr0.echomagic.api.gesture;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum MagicGestureTemplateTarget {
    FIRE("fire", true),
    WATER("water", true),
    EARTH("earth", true),
    AIR("air", true),
    BLOOD("blood", true),
    VOID("void", true),
    LINE("line", false),
    ARC("arc", false),
    CIRCLE("circle", false),
    HOOK("hook", false),
    WEAPON("weapon", false);

    private final String serializedName;
    private final boolean school;

    MagicGestureTemplateTarget(String serializedName, boolean school) {
        this.serializedName = serializedName;
        this.school = school;
    }

    public String serializedName() { return serializedName; }
    public boolean school() { return school; }
    public String translationKey() {
        return school ? "gesture.echo_magic." + serializedName
                : "technique.echo_magic." + serializedName;
    }
    public Optional<MagicGestureSymbol> symbol() {
        return school ? MagicGestureSymbol.fromSerialized(serializedName)
                : Optional.empty();
    }
    public Optional<MagicTechnique> technique() {
        if (school) return Optional.empty();
        return Arrays.stream(MagicTechnique.values())
                .filter(value -> value.serializedName().equals(serializedName)).findFirst();
    }
    public static Optional<MagicGestureTemplateTarget> fromSerialized(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(target -> target.serializedName.equals(normalized)).findFirst();
    }
}
