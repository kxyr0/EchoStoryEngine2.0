package io.github.kxyr0.echovoidmagic.forge;

import io.github.kxyr0.echomagic.api.identity.MagicDamageTypeId;
import io.github.kxyr0.echomagic.api.identity.MagicSchoolId;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.visual.MagicParticleStyleId;

public final class VoidMagicIds {
    public static final SpellId VOID_DAGGERS = new SpellId("echo_void_magic:void_daggers");
    public static final SpellId VOID_STEP = new SpellId("echo_void_magic:void_step");
    public static final MagicSchoolId SCHOOL = new MagicSchoolId("echo_void_magic:void");
    public static final MagicDamageTypeId DAMAGE = new MagicDamageTypeId("echo_void_magic:void_magic");
    public static final MagicParticleStyleId CIRCLE = new MagicParticleStyleId("echo_void_magic:void_circle");
    private VoidMagicIds() { }
}
