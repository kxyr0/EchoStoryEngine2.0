package io.github.kxyr0.echomagicweapons.forge.item;

public enum ArsenalWeaponKind {
    FIRE("fire_spear", "spear", 0xE51818, 1.58F, "thrust", 3, -2.6F),
    WATER("water_whip", "whip", 0x28AAFF, 1.48F, "slash", 2, -2.35F),
    STONE("stone_gauntlets", "gauntlets", 0xB6A477, 0.75F, "fist", 5, -2.7F),
    AIR("air_blade", "blade", 0xD8FFFF, 1.28F, "slash", 1, -2.2F),
    VOID("void_dagger", "dagger", 0x32FF70, 0.72F, "slash", 0, -1.7F);

    private final String id;
    private final String form;
    private final int color;
    private final float length;
    private final String motion;
    private final int damage;
    private final float speed;

    ArsenalWeaponKind(String id, String form, int color, float length, String motion, int damage, float speed) {
        this.id = id;
        this.form = form;
        this.color = color;
        this.length = length;
        this.motion = motion;
        this.damage = damage;
        this.speed = speed;
    }

    public String id() { return id; }
    public String form() { return form; }
    public int color() { return color; }
    public float length() { return length; }
    public String motion() { return motion; }
    public int damage() { return damage; }
    public float speed() { return speed; }
}
