package io.github.kxyr0.echomagic.api.gesture;

public enum MagicGestureModifier {
    NONE("none"),
    DELAY("delay"),
    SPLIT("split");

    private final String serializedName;

    MagicGestureModifier(String serializedName) { this.serializedName = serializedName; }

    public String serializedName() { return serializedName; }
    public String translationKey() { return "modifier.echo_magic." + serializedName; }
}
