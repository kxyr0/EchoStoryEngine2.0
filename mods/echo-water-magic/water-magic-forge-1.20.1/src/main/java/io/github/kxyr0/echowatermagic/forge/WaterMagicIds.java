package io.github.kxyr0.echowatermagic.forge;

import io.github.kxyr0.echomagic.api.identity.MagicDamageTypeId;
import io.github.kxyr0.echomagic.api.identity.MagicSchoolId;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.visual.MagicParticleStyleId;

public final class WaterMagicIds {
    public static final SpellId WATER_BOLT = new SpellId("echo_water_magic:water_bolt");
    public static final SpellId WATER_WAVE = new SpellId("echo_water_magic:water_wave");
    public static final SpellId WATER_RING = new SpellId("echo_water_magic:water_ring");
    public static final MagicSchoolId SCHOOL = new MagicSchoolId("echo_water_magic:water");
    public static final MagicDamageTypeId DAMAGE =
            new MagicDamageTypeId("echo_water_magic:water_magic");
    public static final MagicParticleStyleId CIRCLE =
            new MagicParticleStyleId("echo_water_magic:water_circle");

    private WaterMagicIds() { }
}
