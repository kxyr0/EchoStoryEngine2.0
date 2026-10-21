package io.github.kxyr0.echomagic.api.gesture;

public enum MagicTechnique {
    LINE("line"),
    ARC("arc"),
    CIRCLE("circle"),
    HOOK("hook"),
    POINT("point"),
    WEAPON("weapon");

    private final String serializedName;

    MagicTechnique(String serializedName) { this.serializedName = serializedName; }

    public String serializedName() { return serializedName; }

    public String translationKey() { return "technique.echo_magic." + serializedName; }
}
