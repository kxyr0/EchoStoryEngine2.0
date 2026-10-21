package io.github.kxyr0.echoairmagic.forge;

import io.github.kxyr0.echomagic.api.identity.MagicDamageTypeId;
import io.github.kxyr0.echomagic.api.identity.MagicSchoolId;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.visual.MagicParticleStyleId;

public final class AirMagicIds {
    public static final SpellId AIR_BOLT = new SpellId("echo_air_magic:air_bolt");
    public static final SpellId AIR_ARC = new SpellId("echo_air_magic:air_arc");
    public static final SpellId AIR_RING = new SpellId("echo_air_magic:air_ring");
    public static final MagicSchoolId SCHOOL = new MagicSchoolId("echo_air_magic:air");
    public static final MagicDamageTypeId DAMAGE =
            new MagicDamageTypeId("echo_air_magic:air_magic");
    public static final MagicParticleStyleId CIRCLE =
            new MagicParticleStyleId("echo_air_magic:air_circle");

    private AirMagicIds() { }
}
