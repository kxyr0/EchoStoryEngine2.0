package io.github.kxyr0.echobloodmagic.forge;

import io.github.kxyr0.echomagic.api.identity.MagicDamageTypeId;
import io.github.kxyr0.echomagic.api.identity.MagicSchoolId;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.visual.MagicParticleStyleId;

public final class BloodMagicIds {
    public static final SpellId BLOOD_BOLT = new SpellId("echo_blood_magic:blood_bolt");
    public static final SpellId BLOOD_RAPIER = new SpellId("echo_blood_magic:blood_rapier");
    public static final MagicSchoolId SCHOOL = new MagicSchoolId("echo_blood_magic:blood");
    public static final MagicDamageTypeId DAMAGE =
            new MagicDamageTypeId("echo_blood_magic:blood_magic");
    public static final MagicParticleStyleId CIRCLE =
            new MagicParticleStyleId("echo_blood_magic:blood_circle");

    private BloodMagicIds() { }
}
