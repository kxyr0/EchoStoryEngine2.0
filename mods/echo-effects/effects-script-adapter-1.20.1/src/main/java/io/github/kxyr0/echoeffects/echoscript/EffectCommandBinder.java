package io.github.kxyr0.echoeffects.echoscript;

import io.github.kxyr0.echoeffects.api.identity.EffectId;
import io.github.kxyr0.echoeffects.echoscript.binding.EffectCallSyntax;
import io.github.kxyr0.echoeffects.echoscript.binding.EffectExpressionReader;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.Optional;

public final class EffectCommandBinder {
    private final EffectExpressionReader reader = new EffectExpressionReader();

    public Optional<EffectCommandBinding> bind(InvocationExpressionSyntax invocation) {
        String name = EffectCallSyntax.qualifiedName(invocation);
        return switch (name) {
            case "Effect.ApplyToPlayer" -> Optional.of(apply(invocation));
            case "Effect.RemoveFromPlayer" -> Optional.of(remove(invocation));
            case "Effect.WaitOnPlayer" -> Optional.of(simple(invocation, EffectCommandKind.WAIT_ON_PLAYER));
            case "Effect.IsActiveOnPlayer" -> Optional.of(simple(invocation, EffectCommandKind.IS_ACTIVE_ON_PLAYER));
            case "Effect.HasCompletedOnPlayer" ->
                    Optional.of(simple(invocation, EffectCommandKind.HAS_COMPLETED_ON_PLAYER));
            default -> Optional.empty();
        };
    }

    private EffectCommandBinding apply(InvocationExpressionSyntax invocation) {
        double intensity = EffectCallSyntax.namedNumber(invocation, "intensity").orElse(1.0D);
        return new EffectCommandBinding(EffectCommandKind.APPLY_TO_PLAYER, effectId(invocation), intensity,
                Optional.empty());
    }

    private EffectCommandBinding remove(InvocationExpressionSyntax invocation) {
        Optional<Long> fadeOut = EffectCallSyntax.namedInvocation(invocation, "fadeOut")
                .map(reader::timeTicks);
        return new EffectCommandBinding(EffectCommandKind.REMOVE_FROM_PLAYER, effectId(invocation), 1.0D, fadeOut);
    }

    private EffectCommandBinding simple(InvocationExpressionSyntax invocation, EffectCommandKind kind) {
        return new EffectCommandBinding(kind, effectId(invocation), 1.0D, Optional.empty());
    }

    private EffectId effectId(InvocationExpressionSyntax invocation) {
        return EffectId.of(EffectCallSyntax.positionalString(invocation, 0).orElseThrow());
    }
}
