package io.github.kxyr0.echodialogue.echoscript.binding;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lowers sparse class-level Speaker(...) metadata into the existing alias table.
 */
public final class SpeakerDeclarationReader {
    public void read(InvocationExpressionSyntax call, String namespace, AliasTable aliases,
            List<DialogueDiagnostic> diagnostics) {
        Optional<String> alias = CallSyntax.positionalString(call, 0);
        if (alias.isEmpty()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("ECHO_SCRIPT_SPEAKER_OVERRIDE_INVALID",
                    "Speaker requires a local speaker name.", call));
            return;
        }
        validateNames(call, diagnostics);
        String speaker = CallSyntax.namedString(call, "actor")
                .orElse(namespace + ":" + alias.orElseThrow());
        InvocationExpressionSyntax lowered = aliasCall(call, alias.orElseThrow(), speaker);
        aliases.readAlias(lowered, diagnostics);
        aliases.markOverride(alias.orElseThrow(), CallSyntax.namedArgumentCount(call, "display") > 0,
                CallSyntax.namedArgumentCount(call, "nameColor") > 0,
                CallSyntax.namedArgumentCount(call, "visual") > 0);
    }

    private InvocationExpressionSyntax aliasCall(InvocationExpressionSyntax call, String alias, String speaker) {
        List<ArgumentSyntax> arguments = new ArrayList<>();
        arguments.add(positional(alias, call));
        arguments.add(positional(speaker, call));
        call.arguments().stream().filter(argument -> argument.name().isPresent())
                .filter(argument -> !argument.name().orElseThrow().equals("actor"))
                .forEach(arguments::add);
        return new InvocationExpressionSyntax(call.target(), arguments, call.span());
    }

    private ArgumentSyntax positional(String value, InvocationExpressionSyntax call) {
        return new ArgumentSyntax(Optional.empty(), new StringLiteralExpressionSyntax(value, call.span()), call.span());
    }

    private void validateNames(InvocationExpressionSyntax call, List<DialogueDiagnostic> diagnostics) {
        call.arguments().stream().flatMap(argument -> argument.name().stream())
                .filter(name -> !List.of("actor", "display", "nameColor", "visual").contains(name))
                .forEach(name -> diagnostics.add(DialogueSemanticDiagnostics.semantic(
                        "ECHO_SCRIPT_SPEAKER_OVERRIDE_INVALID", "Unsupported Speaker argument: " + name, call)));
    }
}
