package io.github.kxyr0.echoitem.api.weapon;

import java.util.Locale;

/**
 * Stable authored rarity values supported by N1.
 */
public enum WeaponRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC;

    public static WeaponRarity parse(String value) {
        return WeaponRarity.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
