package io.github.kxyr0.echomagic.echoscript.binding;

import io.github.kxyr0.echomagic.api.cast.SpellBurstDefinition;
import io.github.kxyr0.echomagic.api.cast.SpellCastDefinition;
import io.github.kxyr0.echomagic.api.definition.SpellCooldown;
import io.github.kxyr0.echomagic.api.definition.SpellCoreDefinition;
import io.github.kxyr0.echomagic.api.definition.SpellDefinition;
import io.github.kxyr0.echomagic.api.definition.SpellHitDefinition;
import io.github.kxyr0.echomagic.api.definition.SpellProjectileDefinition;
import io.github.kxyr0.echomagic.api.definition.SpellTrailDefinition;
import io.github.kxyr0.echomagic.api.identity.MagicSchoolId;
import io.github.kxyr0.echomagic.api.identity.SpellDefinitionRevision;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.gesture.MagicTechnique;
import io.github.kxyr0.echomagic.api.projectile.CasterDeathPolicy;
import io.github.kxyr0.echomagic.api.projectile.SpellCollisionDefinition;
import io.github.kxyr0.echomagic.api.projectile.SpellTargetPolicy;

final class SpellDefinitionDraft {
    private final SpellDefinitionRevision revision;
    private SpellId id;
    private String displayName;
    private MagicSchoolId school;
    private MagicTechnique technique = MagicTechnique.LINE;
    private SpellCooldown cooldown;
    private SpellCastDefinition cast;
    private SpellBurstDefinition burst;
    private SpellProjectileDefinition projectile;
    private SpellCollisionDefinition collision;
    private SpellTargetPolicy targets;
    private boolean friendlyFire;
    private SpellTrailDefinition trail;
    private SpellCoreDefinition core;
    private SpellHitDefinition hit;

    SpellDefinitionDraft(SpellDefinitionRevision revision) { this.revision = revision; }
    void id(SpellId value) { id = value; }
    void displayName(String value) { displayName = value; }
    void school(MagicSchoolId value) { school = value; }
    void technique(MagicTechnique value) { technique = value; }
    void cooldown(SpellCooldown value) { cooldown = value; }
    void cast(SpellCastDefinition value) { cast = value; }
    void burst(SpellBurstDefinition value) { burst = value; }
    void projectile(SpellProjectileDefinition value) { projectile = value; }
    void collision(SpellCollisionDefinition value) { collision = value; }
    void targets(SpellTargetPolicy value) { targets = value; }
    void friendlyFire(boolean value) { friendlyFire = value; }
    void trail(SpellTrailDefinition value) { trail = value; }
    void core(SpellCoreDefinition value) { core = value; }
    void hit(SpellHitDefinition value) { hit = value; }

    SpellDefinition definition() {
        return new SpellDefinition(required(id, "Id"), revision, required(displayName, "DisplayName"),
                required(school, "School"), technique, required(cooldown, "Cooldown"), required(cast, "Cast"),
                required(burst, "Burst"), required(projectile, "Projectile"), required(collision, "Collision"),
                required(targets, "Targets"), friendlyFire, required(trail, "Trail"), required(core, "Core"),
                required(hit, "Hit"), CasterDeathPolicy.REMOVE_ACTIVE_PROJECTILES_ON_CASTER_DEATH);
    }

    private <T> T required(T value, String name) {
        if (value == null) throw new IllegalArgumentException("SpellResource requires " + name + "().");
        return value;
    }
}
