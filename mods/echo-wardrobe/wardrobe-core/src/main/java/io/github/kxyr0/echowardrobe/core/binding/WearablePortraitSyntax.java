package io.github.kxyr0.echowardrobe.core.binding;

import io.github.kxyr0.echowardrobe.api.contribution.WearablePortraitStrip;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnostic;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnosticCode;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Optional;

final class WearablePortraitSyntax {
    private WearablePortraitSyntax() {
    }

    static Optional<WearablePortraitStrip> strip(
            InvocationExpressionSyntax call,
            String source,
            List<WardrobeDiagnostic> diagnostics
    ) {
        int[] values = new int[6];
        for (int index = 0; index < values.length; index++) {
            Optional<Integer> value = WearableCallSyntax.positionalInteger(call, index);
            if (value.isEmpty()) {
                diagnostics.add(error("PortraitStrip requires six integer coordinates.", source));
                return Optional.empty();
            }
            values[index] = value.orElseThrow();
        }
        Optional<Boolean> outerLayer = outerLayer(call, source, diagnostics);
        if (outerLayer.isEmpty()) {
            return Optional.empty();
        }
        double pixelXOffset = WearableCallSyntax.positionalDouble(call, 7).orElse(0D);
        double pixelYOffset = WearableCallSyntax.positionalDouble(call, 8).orElse(0D);
        double pixelLeftInset = WearableCallSyntax.positionalDouble(call, 9).orElse(0D);
        double pixelRightInset = WearableCallSyntax.positionalDouble(call, 10).orElse(0D);
        double targetHeightScale = WearableCallSyntax.positionalDouble(call, 11).orElse(1D);
        try {
            return Optional.of(new WearablePortraitStrip(values[0], values[1], values[2], values[3],
                    values[4], values[5], outerLayer.orElseThrow(), pixelXOffset, pixelYOffset,
                    pixelLeftInset, pixelRightInset, targetHeightScale));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(error(exception.getMessage(), source));
            return Optional.empty();
        }
    }

    private static Optional<Boolean> outerLayer(InvocationExpressionSyntax call, String source,
            List<WardrobeDiagnostic> diagnostics) {
        Optional<String> raw = WearableCallSyntax.positionalSymbol(call, 6);
        if (raw.isEmpty() || "BASE".equals(raw.orElseThrow())) {
            return Optional.of(false);
        }
        if ("OUTER".equals(raw.orElseThrow())) {
            return Optional.of(true);
        }
        diagnostics.add(error("PortraitStrip layer must be BASE or OUTER.", source));
        return Optional.empty();
    }

    private static WardrobeDiagnostic error(String message, String source) {
        return WardrobeDiagnostic.error(WardrobeDiagnosticCode.WARDROBE_WEARABLE_INVALID, message, source);
    }
}
