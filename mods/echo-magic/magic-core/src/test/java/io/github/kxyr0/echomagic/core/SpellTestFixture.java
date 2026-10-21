package io.github.kxyr0.echomagic.core;

import io.github.kxyr0.echomagic.api.cast.SpellBurstDefinition;
import io.github.kxyr0.echomagic.api.cast.SpellCastDefinition;
import io.github.kxyr0.echomagic.api.cast.SpellDirection;
import io.github.kxyr0.echomagic.api.cast.SpellHand;
import io.github.kxyr0.echomagic.api.cast.SpellOrigin;
import io.github.kxyr0.echomagic.api.definition.SpellCooldown;
import io.github.kxyr0.echomagic.api.definition.SpellCoreDefinition;
import io.github.kxyr0.echomagic.api.definition.SpellDefinition;
import io.github.kxyr0.echomagic.api.definition.SpellHitDefinition;
import io.github.kxyr0.echomagic.api.definition.SpellProjectileDefinition;
import io.github.kxyr0.echomagic.api.definition.SpellTrailDefinition;
import io.github.kxyr0.echomagic.api.identity.MagicDamageTypeId;
import io.github.kxyr0.echomagic.api.identity.MagicSchoolId;
import io.github.kxyr0.echomagic.api.identity.SpellDefinitionRevision;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.projectile.CasterDeathPolicy;
import io.github.kxyr0.echomagic.api.projectile.SpellBlockCollision;
import io.github.kxyr0.echomagic.api.projectile.SpellCollisionDefinition;
import io.github.kxyr0.echomagic.api.projectile.SpellEntityCollision;
import io.github.kxyr0.echomagic.api.projectile.SpellTargetPolicy;
import io.github.kxyr0.echomagic.api.visual.MagicColor;
import io.github.kxyr0.echomagic.api.visual.MagicParticleStyleId;

public final class SpellTestFixture {
    private SpellTestFixture() { }
    public static SpellDefinition definition(String revision, double speed) {
        MagicParticleStyleId particle = new MagicParticleStyleId("test:circle");
        return new SpellDefinition(new SpellId("test:spell"), new SpellDefinitionRevision(revision), "Test Spell",
                new MagicSchoolId("test:school"), new SpellCooldown(16),
                new SpellCastDefinition(SpellOrigin.PLAYER_EYES, SpellDirection.PLAYER_LOOK, SpellHand.MAIN_HAND, true, 0.35),
                new SpellBurstDefinition(3, 2), new SpellProjectileDefinition(speed, 24, 0.22, 0, 0),
                new SpellCollisionDefinition(SpellBlockCollision.STOP, SpellEntityCollision.FIRST_VALID_TARGET),
                SpellTargetPolicy.LIVING_EXCEPT_CASTER, false,
                new SpellTrailDefinition(particle, 0.18, 0.3F, 8, MagicColor.fromHex("#0F6B32"), 0.9F, 0),
                new SpellCoreDefinition(particle, 0.16F, MagicColor.fromHex("#45FF4A")),
                new SpellHitDefinition(new MagicDamageTypeId("test:magic"), 2),
                CasterDeathPolicy.REMOVE_ACTIVE_PROJECTILES_ON_CASTER_DEATH);
    }
}
