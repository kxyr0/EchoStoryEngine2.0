package io.github.kxyr0.echodialoguevoice.binding;

import io.github.kxyr0.echodialogue.echoscript.api.DialogueLineArgumentBinder;
import io.github.kxyr0.echodialogue.echoscript.api.DialogueLineArgumentBinding;
import io.github.kxyr0.echodialogue.echoscript.api.DialogueLineExtensionCue;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.DecimalLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import io.github.kxyr0.echovoice.api.identity.VoiceFileStem;
import java.math.BigDecimal;
import java.util.List;

/** Binds the exact runtime form voice: Voice("stem", volume: 0.8). */
public final class VoiceLineArgumentBinder implements DialogueLineArgumentBinder {
    @Override
    public String argumentName() {
        return "voice";
    }

    @Override
    public DialogueLineArgumentBinding bind(ExpressionSyntax value) {
        try {
            InvocationExpressionSyntax call = call(value);
            VoiceFileStem stem = stem(call);
            BigDecimal volume = volume(call);
            return DialogueLineArgumentBinding.accepted(List.of(
                    new DialogueLineExtensionCue("voice.stem", stem.value()),
                    new DialogueLineExtensionCue("voice.volume", volume.stripTrailingZeros().toPlainString())));
        } catch (BindingFailure failure) {
            return reject(failure.code, failure.getMessage());
        }
    }

    private InvocationExpressionSyntax call(ExpressionSyntax value) throws BindingFailure {
        if (!(value instanceof InvocationExpressionSyntax call)
                || !(call.target() instanceof NameExpressionSyntax name) || !name.name().equals("Voice")) {
            throw new BindingFailure("VOICE_DSL_EXPECTED",
                    "voice expects Voice(\"file_stem\", volume: 0.0..1.0).");
        }
        if (call.arguments().isEmpty() || call.arguments().size() > 2) {
            throw new BindingFailure("VOICE_DSL_ARGUMENT_COUNT",
                    "Voice requires one stem and optional named volume.");
        }
        return call;
    }

    private VoiceFileStem stem(InvocationExpressionSyntax call) throws BindingFailure {
        ArgumentSyntax argument = call.arguments().get(0);
        if (argument.name().isPresent() || !(argument.value() instanceof StringLiteralExpressionSyntax literal)) {
            throw new BindingFailure("VOICE_DSL_STEM",
                    "Voice filename stem must be the first positional string.");
        }
        try {
            return VoiceFileStem.parse(literal.value());
        } catch (IllegalArgumentException exception) {
            throw new BindingFailure("VOICE_ASSET_FILENAME_INVALID", exception.getMessage());
        }
    }

    private BigDecimal volume(InvocationExpressionSyntax call) throws BindingFailure {
        if (call.arguments().size() == 1) {
            return BigDecimal.ONE;
        }
        ArgumentSyntax argument = call.arguments().get(1);
        if (argument.name().filter("volume"::equals).isEmpty()) {
            throw new BindingFailure("VOICE_DSL_VOLUME_NAME", "The second Voice argument must be named volume.");
        }
        BigDecimal volume = number(argument.value());
        if (volume == null || volume.compareTo(BigDecimal.ZERO) < 0 || volume.compareTo(BigDecimal.ONE) > 0) {
            throw new BindingFailure("VOICE_DSL_VOLUME_RANGE",
                    "Voice volume must be numeric in range 0.0 .. 1.0.");
        }
        return volume;
    }

    private BigDecimal number(ExpressionSyntax value) {
        if (value instanceof DecimalLiteralExpressionSyntax decimal) {
            return decimal.value();
        }
        if (value instanceof IntegerLiteralExpressionSyntax integer) {
            return BigDecimal.valueOf(integer.value());
        }
        return null;
    }

    private DialogueLineArgumentBinding reject(String code, String message) {
        return DialogueLineArgumentBinding.rejected(code, message);
    }

    private static final class BindingFailure extends Exception {
        private final String code;

        private BindingFailure(String code, String message) {
            super(message);
            this.code = code;
        }
    }
}
