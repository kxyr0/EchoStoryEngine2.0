package io.github.kxyr0.echoeffects.echoscript.binding;

import io.github.kxyr0.echoeffects.api.curve.EffectCurveKey;
import io.github.kxyr0.echoeffects.api.curve.EffectFloatCurve;
import io.github.kxyr0.echoeffects.api.curve.EffectInterpolation;
import io.github.kxyr0.echoeffects.api.time.EffectDuration;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;

public final class EffectExpressionReader {
    public EffectDuration duration(InvocationExpressionSyntax invocation) {
        InvocationExpressionSyntax value = EffectCallSyntax.positionalInvocation(invocation, 0)
                .orElseThrow();
        if ("InfiniteDuration".equals(EffectCallSyntax.qualifiedName(value))) {
            return EffectDuration.infinite();
        }
        return EffectDuration.ticks(timeTicks(value));
    }

    public long timeTicks(InvocationExpressionSyntax invocation) {
        double value = EffectCallSyntax.positionalNumber(invocation, 0).orElseThrow();
        return switch (EffectCallSyntax.qualifiedName(invocation)) {
            case "Ticks" -> Math.round(value);
            case "Seconds" -> Math.max(1L, Math.round(value * 20.0D));
            default -> throw new IllegalArgumentException("Unsupported time expression.");
        };
    }

    public EffectFloatCurve curve(InvocationExpressionSyntax invocation) {
        List<EffectCurveKey> keys = invocation.arguments().stream()
                .map(ArgumentSyntax::value)
                .map(InvocationExpressionSyntax.class::cast)
                .map(this::key)
                .toList();
        return new EffectFloatCurve(keys);
    }

    private EffectCurveKey key(InvocationExpressionSyntax invocation) {
        long tick = EffectCallSyntax.positionalInvocation(invocation, 0)
                .map(this::timeTicks)
                .orElseThrow();
        double value = EffectCallSyntax.positionalNumber(invocation, 1).orElseThrow();
        EffectInterpolation interpolation = invocation.arguments().size() > 2
                ? EffectInterpolation.valueOf(EffectCallSyntax.nameValue(invocation.arguments().get(2)).orElseThrow())
                : EffectInterpolation.LINEAR;
        return new EffectCurveKey(tick, value, interpolation);
    }
}
