package io.github.kxyr0.echotimehud.api;

public enum HudAnchor {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT;

    public boolean left() { return this == TOP_LEFT || this == CENTER_LEFT || this == BOTTOM_LEFT; }
    public boolean right() { return this == TOP_RIGHT || this == CENTER_RIGHT || this == BOTTOM_RIGHT; }
    public boolean top() { return this == TOP_LEFT || this == TOP_CENTER || this == TOP_RIGHT; }
    public boolean bottom() { return this == BOTTOM_LEFT || this == BOTTOM_CENTER || this == BOTTOM_RIGHT; }
}

