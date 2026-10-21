package io.github.kxyr0.echomagic.echoscript.binding;

import io.github.kxyr0.echomagic.api.cast.SpellBurstDefinition;
import io.github.kxyr0.echomagic.api.cast.SpellCastDefinition;
import io.github.kxyr0.echomagic.api.cast.SpellDirection;
import io.github.kxyr0.echomagic.api.cast.SpellHand;
import io.github.kxyr0.echomagic.api.cast.SpellOrigin;
import io.github.kxyr0.echomagic.api.definition.SpellCooldown;
import io.github.kxyr0.echomagic.api.definition.SpellCoreDefinition;
import io.github.kxyr0.echomagic.api.definition.SpellHitDefinition;
import io.github.kxyr0.echomagic.api.definition.SpellProjectileDefinition;
import io.github.kxyr0.echomagic.api.definition.SpellTrailDefinition;
import io.github.kxyr0.echomagic.api.identity.MagicDamageTypeId;
import io.github.kxyr0.echomagic.api.hit.SpellKnockbackDirection;
import io.github.kxyr0.echomagic.api.projectile.SpellBlockCollision;
import io.github.kxyr0.echomagic.api.projectile.SpellCollisionDefinition;
import io.github.kxyr0.echomagic.api.projectile.SpellEntityCollision;
import io.github.kxyr0.echomagic.api.visual.MagicColor;
import io.github.kxyr0.echomagic.api.visual.MagicParticleStyleId;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;

final class SpellSectionBinder {
    private final SpellValueReader values;

    SpellSectionBinder(SpellValueReader values) { this.values = values; }

    SpellCooldown cooldown(InvocationExpressionSyntax call) {
        return new SpellCooldown(ticks(values.invocation(call, "duration", 0)));
    }

    SpellCastDefinition cast(InvocationExpressionSyntax call) {
        List<InvocationExpressionSyntax> items = nested(call);
        SpellOrigin origin = SpellOrigin.valueOf(enumFrom(items, "Origin"));
        SpellDirection direction = SpellDirection.valueOf(enumFrom(items, "Direction"));
        SpellHand hand = SpellHand.valueOf(enumFrom(items, "Hand"));
        boolean swing = boolFrom(items, "SwingHand");
        return new SpellCastDefinition(origin, direction, hand, swing, 0.35D);
    }

    SpellBurstDefinition burst(InvocationExpressionSyntax call) {
        double spread = values.optionalInvocation(call, "spread", 2).map(this::degrees).orElse(0.0D);
        return new SpellBurstDefinition((int) values.number(call, "count", 0),
                ticks(values.invocation(call, "interval", 1)), spread);
    }

    SpellProjectileDefinition projectile(InvocationExpressionSyntax call) {
        double speed = values.number(values.invocation(call, "speed", 0), "value", 0);
        double range = values.number(values.invocation(call, "range", 1), "value", 0);
        return new SpellProjectileDefinition(speed, range, values.number(call, "radius", 2),
                values.number(call, "gravity", 3), (int) values.number(call, "pierce", 4));
    }

    SpellCollisionDefinition collision(InvocationExpressionSyntax call) {
        return new SpellCollisionDefinition(SpellBlockCollision.valueOf(values.enumName(call, "blocks", 0)),
                SpellEntityCollision.valueOf(values.enumName(call, "entities", 1)));
    }

    SpellTrailDefinition trail(InvocationExpressionSyntax call) {
        MagicColor color = color(call, "color", 4);
        return new SpellTrailDefinition(new MagicParticleStyleId(values.string(call, "particle", 0)),
                values.number(call, "spacing", 1), (float) values.number(call, "size", 2),
                (int) ticks(values.invocation(call, "lifetime", 3)), color,
                optionalColor(call, "vanillaColor", 7, color),
                (float) values.number(call, "alphaStart", 5), (float) values.number(call, "alphaEnd", 6));
    }

    SpellCoreDefinition core(InvocationExpressionSyntax call) {
        MagicColor color = color(call, "color", 2);
        return new SpellCoreDefinition(new MagicParticleStyleId(values.string(call, "particle", 0)),
                (float) values.number(call, "size", 1), color,
                optionalColor(call, "vanillaColor", 3, color));
    }

    SpellHitDefinition hit(InvocationExpressionSyntax call) {
        List<InvocationExpressionSyntax> items = nested(call);
        InvocationExpressionSyntax damage = find(items, "Damage");
        int ignite = findOptional(items, "Ignite").map(value -> (int) ticks(
                values.invocation(value, "duration", 0))).orElse(0);
        boolean extinguish = findOptional(items, "Extinguish")
                .map(value -> values.bool(value, "enabled", 0)).orElse(false);
        double knockback = findOptional(items, "Knockback")
                .map(value -> values.number(value, "strength", 0)).orElse(0.0D);
        SpellKnockbackDirection direction = findOptional(items, "Knockback")
                .map(value -> SpellKnockbackDirection.valueOf(values.enumName(value, "direction", 1)))
                .orElse(SpellKnockbackDirection.FORWARD);
        boolean deflect = findOptional(items, "DeflectProjectiles")
                .map(value -> values.bool(value, "enabled", 0)).orElse(false);
        return new SpellHitDefinition(new MagicDamageTypeId(values.string(damage, "type", 0)),
                values.number(damage, "amount", 1), ignite, extinguish, knockback, direction, deflect);
    }

    private MagicColor color(InvocationExpressionSyntax call, String name, int position) {
        return MagicColor.fromHex(values.string(values.invocation(call, name, position), "hex", 0));
    }

    private MagicColor optionalColor(InvocationExpressionSyntax call, String name, int position,
                                     MagicColor fallback) {
        return values.optionalInvocation(call, name, position)
                .map(value -> MagicColor.fromHex(values.string(value, "hex", 0))).orElse(fallback);
    }

    private long ticks(InvocationExpressionSyntax call) {
        if (!"Ticks".equals(SpellSyntax.name(call))) throw new IllegalArgumentException("Expected Ticks().");
        return Math.round(values.number(call, "value", 0));
    }

    private double degrees(InvocationExpressionSyntax call) {
        if (!"Degrees".equals(SpellSyntax.name(call))) throw new IllegalArgumentException("Expected Degrees().");
        return values.number(call, "value", 0);
    }

    private String enumFrom(List<InvocationExpressionSyntax> items, String name) {
        InvocationExpressionSyntax call = find(items, name);
        return values.enumName(call, "value", 0);
    }

    private boolean boolFrom(List<InvocationExpressionSyntax> items, String name) {
        return values.bool(find(items, name), "value", 0);
    }

    private InvocationExpressionSyntax find(List<InvocationExpressionSyntax> items, String name) {
        return items.stream().filter(value -> name.equals(SpellSyntax.name(value))).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cast requires " + name + "()."));
    }

    private java.util.Optional<InvocationExpressionSyntax> findOptional(List<InvocationExpressionSyntax> items,
                                                                         String name) {
        return items.stream().filter(value -> name.equals(SpellSyntax.name(value))).findFirst();
    }

    private List<InvocationExpressionSyntax> nested(InvocationExpressionSyntax call) {
        return call.arguments().stream().map(ArgumentSyntax::value).map(InvocationExpressionSyntax.class::cast).toList();
    }
}
