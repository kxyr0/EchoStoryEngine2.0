package io.github.kxyr0.echomagicweapons.forge;

import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagicweapons.forge.item.ArsenalWeaponKind;
import java.util.EnumMap;
import java.util.Map;

public final class ArsenalIds {
    public static final SpellId FIRE_SPEAR = new SpellId("echo_magic_weapons:fire_spear");
    public static final SpellId WATER_WHIP = new SpellId("echo_magic_weapons:water_whip");
    public static final SpellId STONE_GAUNTLETS = new SpellId("echo_magic_weapons:stone_gauntlets");
    public static final SpellId AIR_BLADE = new SpellId("echo_magic_weapons:air_blade");
    public static final SpellId VOID_DAGGER = new SpellId("echo_magic_weapons:void_dagger");
    private static final Map<ArsenalWeaponKind, SpellId> SPELLS = new EnumMap<>(ArsenalWeaponKind.class);

    static {
        SPELLS.put(ArsenalWeaponKind.FIRE, FIRE_SPEAR);
        SPELLS.put(ArsenalWeaponKind.WATER, WATER_WHIP);
        SPELLS.put(ArsenalWeaponKind.STONE, STONE_GAUNTLETS);
        SPELLS.put(ArsenalWeaponKind.AIR, AIR_BLADE);
        SPELLS.put(ArsenalWeaponKind.VOID, VOID_DAGGER);
    }

    private ArsenalIds() { }

    public static SpellId spell(ArsenalWeaponKind kind) {
        return SPELLS.get(kind);
    }

    public static ArsenalWeaponKind kind(SpellId spell) {
        return SPELLS.entrySet().stream().filter(entry -> entry.getValue().equals(spell))
                .map(Map.Entry::getKey).findFirst().orElse(null);
    }
}
