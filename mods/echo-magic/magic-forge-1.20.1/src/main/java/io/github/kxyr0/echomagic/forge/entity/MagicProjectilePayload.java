package io.github.kxyr0.echomagic.forge.entity;

import io.github.kxyr0.echomagic.api.definition.SpellDefinition;
import io.github.kxyr0.echomagic.api.identity.SpellCastId;
import io.github.kxyr0.echomagic.api.identity.SpellProjectileId;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

public record MagicProjectilePayload(String projectileId, String castId, String spellId, String definitionRevision,
        UUID caster, double range, double radius, double gravity, String damageType, double damageAmount, boolean friendlyFire,
        int igniteTicks, boolean extinguish, double knockback, String knockbackDirection,
        boolean deflectProjectiles,
        String particleStyle, String coreParticleStyle, double trailSpacing, float trailSize, int trailLifetime,
        float trailRed, float trailGreen, float trailBlue, float vanillaTrailRed, float vanillaTrailGreen,
        float vanillaTrailBlue, float alphaStart, float alphaEnd, float coreSize, float coreRed, float coreGreen,
        float coreBlue, float vanillaCoreRed, float vanillaCoreGreen, float vanillaCoreBlue) {
    public static MagicProjectilePayload from(SpellProjectileId projectile, SpellCastId cast, UUID caster,
                                              SpellDefinition definition) {
        var trail = definition.trail();
        var core = definition.core();
        return new MagicProjectilePayload(projectile.value(), cast.value(), definition.id().value(),
                definition.revision().value(), caster, definition.projectile().range(), definition.projectile().radius(),
                definition.projectile().gravity(), definition.hit().damageType().value(), definition.hit().amount(),
                definition.friendlyFire(), definition.hit().igniteTicks(), definition.hit().extinguish(),
                definition.hit().knockback(), definition.hit().knockbackDirection().name(),
                definition.hit().deflectProjectiles(),
                trail.particle().value(), core.particle().value(), trail.spacing(), trail.size(), trail.lifetimeTicks(),
                trail.color().red(), trail.color().green(), trail.color().blue(), trail.vanillaColor().red(),
                trail.vanillaColor().green(), trail.vanillaColor().blue(), trail.alphaStart(), trail.alphaEnd(),
                core.size(), core.color().red(), core.color().green(), core.color().blue(), core.vanillaColor().red(),
                core.vanillaColor().green(), core.vanillaColor().blue());
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("projectile", projectileId); tag.putString("cast", castId); tag.putString("spell", spellId);
        tag.putString("revision", definitionRevision); tag.putUUID("caster", caster); tag.putDouble("range", range);
        tag.putDouble("radius", radius); tag.putDouble("gravity", gravity); tag.putString("damageType", damageType);
        tag.putDouble("damage", damageAmount); tag.putBoolean("friendlyFire", friendlyFire);
        tag.putInt("igniteTicks", igniteTicks); tag.putBoolean("extinguish", extinguish);
        tag.putDouble("knockback", knockback); tag.putString("knockbackDirection", knockbackDirection);
        tag.putBoolean("deflectProjectiles", deflectProjectiles); tag.putString("particle", particleStyle);
        tag.putString("coreParticle", coreParticleStyle);
        tag.putDouble("spacing", trailSpacing); tag.putFloat("trailSize", trailSize); tag.putInt("trailLife", trailLifetime);
        tag.putFloat("tr", trailRed); tag.putFloat("tg", trailGreen); tag.putFloat("tb", trailBlue);
        tag.putFloat("vtr", vanillaTrailRed); tag.putFloat("vtg", vanillaTrailGreen);
        tag.putFloat("vtb", vanillaTrailBlue);
        tag.putFloat("a0", alphaStart); tag.putFloat("a1", alphaEnd); tag.putFloat("coreSize", coreSize);
        tag.putFloat("cr", coreRed); tag.putFloat("cg", coreGreen); tag.putFloat("cb", coreBlue);
        tag.putFloat("vcr", vanillaCoreRed); tag.putFloat("vcg", vanillaCoreGreen);
        tag.putFloat("vcb", vanillaCoreBlue);
        return tag;
    }

    public static MagicProjectilePayload fromTag(CompoundTag tag) {
        return new MagicProjectilePayload(tag.getString("projectile"), tag.getString("cast"), tag.getString("spell"),
                tag.getString("revision"), tag.getUUID("caster"), tag.getDouble("range"), tag.getDouble("radius"),
                tag.getDouble("gravity"), tag.getString("damageType"), tag.getDouble("damage"), tag.getBoolean("friendlyFire"),
                tag.getInt("igniteTicks"), tag.getBoolean("extinguish"), tag.getDouble("knockback"),
                tag.contains("knockbackDirection") ? tag.getString("knockbackDirection") : "FORWARD",
                tag.getBoolean("deflectProjectiles"), tag.getString("particle"),
                tag.contains("coreParticle") ? tag.getString("coreParticle") : tag.getString("particle"),
                tag.getDouble("spacing"), tag.getFloat("trailSize"),
                tag.getInt("trailLife"), tag.getFloat("tr"), tag.getFloat("tg"), tag.getFloat("tb"),
                tag.contains("vtr") ? tag.getFloat("vtr") : tag.getFloat("tr"),
                tag.contains("vtg") ? tag.getFloat("vtg") : tag.getFloat("tg"),
                tag.contains("vtb") ? tag.getFloat("vtb") : tag.getFloat("tb"),
                tag.getFloat("a0"), tag.getFloat("a1"), tag.getFloat("coreSize"), tag.getFloat("cr"),
                tag.getFloat("cg"), tag.getFloat("cb"),
                tag.contains("vcr") ? tag.getFloat("vcr") : tag.getFloat("cr"),
                tag.contains("vcg") ? tag.getFloat("vcg") : tag.getFloat("cg"),
                tag.contains("vcb") ? tag.getFloat("vcb") : tag.getFloat("cb"));
    }
}
