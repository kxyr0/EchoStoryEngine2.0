package io.github.kxyr0.echofiremagic.forge;

import io.github.kxyr0.echomagic.api.identity.MagicDamageTypeId;
import io.github.kxyr0.echomagic.api.identity.MagicSchoolId;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.visual.MagicParticleStyleId;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class FireMagicIds {
    public static final SpellId FIRE_BOLT = new SpellId("echo_fire_magic:fire_bolt");
    public static final SpellId FIRE_SWEEP = new SpellId("echo_fire_magic:fire_sweep");
    public static final SpellId FIRE_RING = new SpellId("echo_fire_magic:fire_ring");
    public static final SpellId BLACKSMITH_HAND =
            new SpellId("echo_fire_magic:blacksmith_hand");
    public static final MagicSchoolId SCHOOL = new MagicSchoolId("echo_fire_magic:fire");
    public static final MagicDamageTypeId DAMAGE =
            new MagicDamageTypeId("echo_fire_magic:fire_magic");
    public static final MagicParticleStyleId FLAME =
            new MagicParticleStyleId("echo_fire_magic:fire_flame");
    public static final TagKey<Block> BLACKSMITH_HEATABLE = TagKey.create(Registries.BLOCK,
            new net.minecraft.resources.ResourceLocation("echo_fire_magic", "blacksmith_heatable"));

    private FireMagicIds() { }
}
