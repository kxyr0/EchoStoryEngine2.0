package io.github.kxyr0.echomagic.api.definition;

import io.github.kxyr0.echomagic.api.cast.SpellBurstDefinition;
import io.github.kxyr0.echomagic.api.cast.SpellCastDefinition;
import io.github.kxyr0.echomagic.api.gesture.MagicTechnique;
import io.github.kxyr0.echomagic.api.identity.MagicSchoolId;
import io.github.kxyr0.echomagic.api.identity.SpellDefinitionRevision;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.projectile.CasterDeathPolicy;
import io.github.kxyr0.echomagic.api.projectile.SpellCollisionDefinition;
import io.github.kxyr0.echomagic.api.projectile.SpellTargetPolicy;
import java.util.Objects;

public record SpellDefinition(SpellId id, SpellDefinitionRevision revision, String displayName,
        MagicSchoolId school, MagicTechnique technique, SpellCooldown cooldown, SpellCastDefinition cast,
        SpellBurstDefinition burst,
        SpellProjectileDefinition projectile, SpellCollisionDefinition collision, SpellTargetPolicy targets,
        boolean friendlyFire, SpellTrailDefinition trail, SpellCoreDefinition core, SpellHitDefinition hit,
        CasterDeathPolicy casterDeathPolicy) {
    public SpellDefinition(SpellId id, SpellDefinitionRevision revision, String displayName, MagicSchoolId school,
            SpellCooldown cooldown, SpellCastDefinition cast, SpellBurstDefinition burst,
            SpellProjectileDefinition projectile, SpellCollisionDefinition collision, SpellTargetPolicy targets,
            boolean friendlyFire, SpellTrailDefinition trail, SpellCoreDefinition core, SpellHitDefinition hit,
            CasterDeathPolicy casterDeathPolicy) {
        this(id, revision, displayName, school, MagicTechnique.LINE, cooldown, cast, burst, projectile, collision,
                targets, friendlyFire, trail, core, hit, casterDeathPolicy);
    }

    public SpellDefinition {
        id = Objects.requireNonNull(id, "id");
        revision = Objects.requireNonNull(revision, "revision");
        displayName = Objects.requireNonNull(displayName, "displayName");
        school = Objects.requireNonNull(school, "school");
        technique = Objects.requireNonNull(technique, "technique");
        cooldown = Objects.requireNonNull(cooldown, "cooldown");
        cast = Objects.requireNonNull(cast, "cast");
        burst = Objects.requireNonNull(burst, "burst");
        projectile = Objects.requireNonNull(projectile, "projectile");
        collision = Objects.requireNonNull(collision, "collision");
        targets = Objects.requireNonNull(targets, "targets");
        trail = Objects.requireNonNull(trail, "trail");
        core = Objects.requireNonNull(core, "core");
        hit = Objects.requireNonNull(hit, "hit");
        casterDeathPolicy = Objects.requireNonNull(casterDeathPolicy, "casterDeathPolicy");
        if (displayName.isBlank()) throw new IllegalArgumentException("Display name must not be blank.");
    }
}
