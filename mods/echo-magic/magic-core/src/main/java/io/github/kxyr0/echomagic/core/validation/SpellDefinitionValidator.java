package io.github.kxyr0.echomagic.core.validation;

import io.github.kxyr0.echomagic.api.definition.SpellDefinition;
import io.github.kxyr0.echomagic.api.diagnostic.MagicDiagnostic;
import io.github.kxyr0.echomagic.api.diagnostic.MagicDiagnosticCode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class SpellDefinitionValidator {
    private final Supplier<SpellSafetyLimits> limits;

    public SpellDefinitionValidator(SpellSafetyLimits limits) { this(() -> limits); }
    public SpellDefinitionValidator(Supplier<SpellSafetyLimits> limits) { this.limits = limits; }

    public List<MagicDiagnostic> validate(SpellDefinition definition, String source) {
        List<MagicDiagnostic> result = new ArrayList<>();
        SpellSafetyLimits current = limits.get();
        check(definition.projectile().speed() <= current.maximumSpeed(), "Projectile speed exceeds server limit.", source, result);
        check(definition.projectile().range() <= current.maximumRange(), "Projectile range exceeds server limit.", source, result);
        check(definition.burst().count() <= current.maximumBurst(), "Burst count exceeds server limit.", source, result);
        check(definition.hit().amount() <= current.maximumDamage(), "Damage exceeds server limit.", source, result);
        check(definition.revision().value() != null && !definition.revision().value().isBlank(),
                "Definition revision is missing.", source, result);
        return List.copyOf(result);
    }

    private void check(boolean valid, String message, String source, List<MagicDiagnostic> result) {
        if (!valid) result.add(MagicDiagnostic.error(MagicDiagnosticCode.MAGIC_SPELL_INVALID, message, source));
    }
}
