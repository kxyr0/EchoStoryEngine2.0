package io.github.kxyr0.echodialogue.echoscript.binding.node;

import io.github.kxyr0.echodialogue.echoscript.binding.AliasTable;
import io.github.kxyr0.echodialogue.echoscript.binding.CallSyntax;
import io.github.kxyr0.echodialogue.echoscript.binding.MethodCatalog;
import io.github.kxyr0.echodialogue.echoscript.binding.node.animation.LineAnimationCueReader;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import io.github.kxyr0.echodialogue.echoscript.api.DialogueLineArgumentBinder;
import io.github.kxyr0.echodialogue.echoscript.api.DialogueLineArgumentBinding;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads compact Line(...) calls and generic line call chains.
 */
final class NodeLineReader {
    private final Map<String, DialogueLineArgumentBinder> extensions;
    private final LineAnimationCueReader animationReader = new LineAnimationCueReader();

    NodeLineReader() {
        this(List.of());
    }

    NodeLineReader(List<DialogueLineArgumentBinder> extensions) {
        this.extensions = new LinkedHashMap<>();
        for (DialogueLineArgumentBinder extension : extensions) {
            DialogueLineArgumentBinder previous = this.extensions.putIfAbsent(extension.argumentName(), extension);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate Line argument binder: " + extension.argumentName());
            }
        }
    }

    Optional<CompiledDialogueLine> readLine(DialogueNodeId nodeId, int lineIndex, InvocationExpressionSyntax call,
            LineContext context) {
        List<CallSyntax.InvocationSegment> chain = CallSyntax.chain(call);
        InvocationExpressionSyntax lineCall = chain.get(0).invocation();
        Optional<String> alias = CallSyntax.positionalString(lineCall, 0);
        Optional<String> text = CallSyntax.positionalString(lineCall, 1);
        if (alias.isEmpty() || text.isEmpty()) {
            context.diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-010",
                    "Line requires speaker alias and text.", lineCall));
            return Optional.empty();
        }
        return readLine(new LineSeed(nodeId, lineIndex, chain, alias.orElseThrow(), text.orElseThrow()), context);
    }

    private Optional<CompiledDialogueLine> readLine(LineSeed seed, LineContext context) {
        InvocationExpressionSyntax lineCall = seed.chain.get(0).invocation();
        Optional<AliasTable.SpeakerBinding> binding = context.aliases.resolve(seed.alias)
                .or(() -> implicitBinding(seed.alias, context.implicitSpeakerNamespace));
        if (binding.isEmpty()) {
            context.diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-011",
                    "Line references unknown speaker alias: " + seed.alias, lineCall));
            return Optional.empty();
        }
        List<CompiledDialogueLine.Cue> cues = new ArrayList<>();
        readLineProperties(lineCall, cues, context.diagnostics);
        animationReader.read(seed.chain, context.methods, cues, context.diagnostics);
        AliasTable.SpeakerBinding speaker = binding.orElseThrow();
        addSpeakerOverrideCues(speaker, cues);
        return Optional.of(new CompiledDialogueLine(seed.nodeId, seed.lineIndex, speaker.speakerId(),
                speaker.displayName(), speaker.nameColor(), speaker.visual(), seed.text,
                readLineKey(lineCall, context.diagnostics), CallSyntax.namedBoolean(lineCall, "thought").orElse(false),
                cues));
    }

    private Optional<AliasTable.SpeakerBinding> implicitBinding(String alias, Optional<String> namespace) {
        return namespace.map(value -> new AliasTable.SpeakerBinding(SpeakerId.parse(value + ":" + alias), alias,
                Optional.empty(), Optional.empty(), false, false, false));
    }

    private void addSpeakerOverrideCues(AliasTable.SpeakerBinding speaker, List<CompiledDialogueLine.Cue> cues) {
        if (speaker.displayOverride()) {
            cues.add(new CompiledDialogueLine.Cue("speakerOverride.display", "true"));
        }
        if (speaker.nameColorOverride()) {
            cues.add(new CompiledDialogueLine.Cue("speakerOverride.nameColor", "true"));
        }
        if (speaker.visualOverride()) {
            cues.add(new CompiledDialogueLine.Cue("speakerOverride.visual", "true"));
        }
    }

    private Optional<String> readLineKey(InvocationExpressionSyntax call, List<DialogueDiagnostic> diagnostics) {
        Optional<String> positional = CallSyntax.positionalString(call, 2);
        Optional<String> named = CallSyntax.namedString(call, "key");
        if (positional.isPresent() && named.isPresent()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("DIALOGUE_DUPLICATE_LINE_KEY",
                    "Line contains both positional and named key.", call));
        }
        return positional.or(() -> named);
    }

    private void readLineProperties(InvocationExpressionSyntax call, List<CompiledDialogueLine.Cue> cues,
            List<DialogueDiagnostic> diagnostics) {
        call.arguments().stream()
                .filter(argument -> argument.name().isPresent())
                .filter(argument -> !argument.name().orElseThrow().equals("key"))
                .filter(argument -> !argument.name().orElseThrow().equals("thought"))
                .forEach(argument -> readLineProperty(call, argument.name().orElseThrow(), argument.value(), cues,
                        diagnostics));
    }

    private void readLineProperty(InvocationExpressionSyntax call, String name, ExpressionSyntax value,
            List<CompiledDialogueLine.Cue> cues, List<DialogueDiagnostic> diagnostics) {
        if (name.equals("visual")) {
            diagnostics.add(DialogueSemanticDiagnostics.unsupported("Line argument 'visual'", call));
            return;
        }
        Optional<String> simple = CallSyntax.simpleValue(value);
        if (simple.isPresent()) {
            cues.add(new CompiledDialogueLine.Cue("property." + name, simple.orElseThrow()));
            return;
        }
        DialogueLineArgumentBinder extension = extensions.get(name);
        if (extension == null) {
            diagnostics.add(DialogueSemanticDiagnostics.unsupported("Line argument '" + name + "'", value));
            return;
        }
        applyExtension(extension.bind(value), value, cues, diagnostics);
    }

    private void applyExtension(DialogueLineArgumentBinding binding, ExpressionSyntax value,
            List<CompiledDialogueLine.Cue> cues, List<DialogueDiagnostic> diagnostics) {
        binding.cues().forEach(cue -> cues.add(new CompiledDialogueLine.Cue(cue.name(), cue.value())));
        binding.issues().forEach(issue -> diagnostics.add(new DialogueDiagnostic(
                issue.code(), issue.severity(), DiagnosticCategory.SEMANTIC_VALIDATION, issue.message(),
                DiagnosticSubject.source(value.span().sourceId().value(), value.span().start().line(),
                        value.span().start().column()))));
    }

    record LineContext(AliasTable aliases, MethodCatalog methods, List<DialogueDiagnostic> diagnostics,
            Optional<String> implicitSpeakerNamespace) {
    }

    private record LineSeed(
            DialogueNodeId nodeId,
            int lineIndex,
            List<CallSyntax.InvocationSegment> chain,
            String alias,
            String text
    ) {
    }
}
