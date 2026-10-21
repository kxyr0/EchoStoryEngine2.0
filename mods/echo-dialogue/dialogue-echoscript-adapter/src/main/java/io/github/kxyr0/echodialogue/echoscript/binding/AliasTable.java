package io.github.kxyr0.echodialogue.echoscript.binding;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.visual.SpeakerNameColor;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualRef;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Semantic speaker alias table built from Init().
 */
public final class AliasTable {
    private static final String NAME_COLOR = "nameColor";

    private final Map<String, SpeakerBinding> aliases = new HashMap<>();

    boolean add(String alias, SpeakerId speakerId, String displayName, Optional<SpeakerVisualRef> visual) {
        return add(alias, speakerId, displayName, Optional.empty(), visual);
    }

    boolean add(String alias, SpeakerId speakerId, String displayName, Optional<SpeakerNameColor> nameColor,
            Optional<SpeakerVisualRef> visual) {
        if (aliases.containsKey(alias)) {
            return false;
        }
        aliases.put(alias, new SpeakerBinding(speakerId, displayName, nameColor, visual, false, false, false));
        return true;
    }

    void markOverride(String alias, boolean display, boolean nameColor, boolean visual) {
        SpeakerBinding current = aliases.get(alias);
        if (current != null) {
            aliases.put(alias, new SpeakerBinding(current.speakerId(), current.displayName(), current.nameColor(),
                    current.visual(), display, nameColor, visual));
        }
    }

    public Optional<SpeakerBinding> resolve(String alias) {
        return Optional.ofNullable(aliases.get(alias));
    }

    void readAlias(InvocationExpressionSyntax call, List<DialogueDiagnostic> diagnostics) {
        unsupportedAliasArguments(call, diagnostics);
        Optional<String> alias = CallSyntax.positionalString(call, 0);
        Optional<String> speaker = CallSyntax.positionalString(call, 1);
        if (alias.isEmpty() || speaker.isEmpty()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-005",
                    "Alias requires alias and SpeakerId.", call));
            return;
        }
        addAlias(call, diagnostics, alias.orElseThrow(), speaker.orElseThrow());
    }

    private void addAlias(
            InvocationExpressionSyntax call,
            List<DialogueDiagnostic> diagnostics,
            String alias,
            String speaker
    ) {
        Optional<SpeakerId> speakerId = safeSpeakerId(speaker, diagnostics);
        String display = CallSyntax.namedString(call, "display").orElse(alias);
        Optional<SpeakerNameColor> nameColor = readNameColor(call, diagnostics);
        Optional<SpeakerVisualRef> visual = readVisual(call, diagnostics);
        if (speakerId.isPresent() && !add(alias, speakerId.orElseThrow(), display, nameColor, visual)) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-006",
                    "Speaker alias declared more than once: " + alias, call));
        }
    }

    private void unsupportedAliasArguments(InvocationExpressionSyntax call, List<DialogueDiagnostic> diagnostics) {
        call.arguments().stream()
                .flatMap(argument -> argument.name().stream())
                .filter(name -> !name.equals("display"))
                .filter(name -> !name.equals(NAME_COLOR))
                .filter(name -> !name.equals("visual"))
                .forEach(name -> diagnostics.add(DialogueSemanticDiagnostics.unsupported(
                        "Alias argument '" + name + "'", call)));
    }

    private Optional<SpeakerNameColor> readNameColor(
            InvocationExpressionSyntax aliasCall,
            List<DialogueDiagnostic> diagnostics
    ) {
        if (CallSyntax.namedArgumentCount(aliasCall, NAME_COLOR) > 1) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-046",
                    "Alias contains more than one nameColor argument.", aliasCall));
            return Optional.empty();
        }
        if (CallSyntax.namedArgumentCount(aliasCall, NAME_COLOR) == 0) {
            return Optional.empty();
        }
        Optional<InvocationExpressionSyntax> colorCall = CallSyntax.namedInvocation(aliasCall, NAME_COLOR);
        if (colorCall.isEmpty()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-047",
                    "Alias nameColor must be nameColor(\"#RRGGBB\") or opaque nameColor(\"#RRGGBBAA\").",
                    aliasCall));
            return Optional.empty();
        }
        return readNameColorCall(colorCall.orElseThrow(), diagnostics);
    }

    private Optional<SpeakerNameColor> readNameColorCall(
            InvocationExpressionSyntax colorCall,
            List<DialogueDiagnostic> diagnostics
    ) {
        Optional<String> raw = CallSyntax.positionalString(colorCall, 0);
        if (raw.isEmpty() || colorCall.arguments().size() != 1) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-048",
                    "nameColor intrinsic requires one hex string.", colorCall));
            return Optional.empty();
        }
        if (!CallSyntax.name(colorCall).equals(NAME_COLOR) && !CallSyntax.name(colorCall).equals("NameColor")) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-048",
                    "Alias nameColor intrinsic is not supported: " + CallSyntax.name(colorCall), colorCall));
            return Optional.empty();
        }
        try {
            return Optional.of(SpeakerNameColor.parse(raw.orElseThrow()));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-049", exception.getMessage(), colorCall));
            return Optional.empty();
        }
    }

    private Optional<SpeakerVisualRef> readVisual(
            InvocationExpressionSyntax aliasCall,
            List<DialogueDiagnostic> diagnostics
    ) {
        if (CallSyntax.namedArgumentCount(aliasCall, "visual") > 1) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-028",
                    "Alias contains more than one visual argument.", aliasCall));
            return Optional.empty();
        }
        if (CallSyntax.namedArgumentCount(aliasCall, "visual") == 0) {
            return Optional.empty();
        }
        Optional<InvocationExpressionSyntax> visualCall = CallSyntax.namedInvocation(aliasCall, "visual");
        if (visualCall.isEmpty()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-029",
                    "Alias visual must be Portrait(\"namespace:path\") or SkinHead(\"namespace:path\").", aliasCall));
            return Optional.empty();
        }
        return readVisualCall(visualCall.orElseThrow(), diagnostics);
    }

    private Optional<SpeakerVisualRef> readVisualCall(
            InvocationExpressionSyntax visualCall,
            List<DialogueDiagnostic> diagnostics
    ) {
        Optional<String> raw = CallSyntax.positionalString(visualCall, 0);
        if (raw.isEmpty() || visualCall.arguments().size() != 1) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-030",
                    "Visual intrinsic requires one namespaced ID.", visualCall));
            return Optional.empty();
        }
        try {
            return switch (CallSyntax.name(visualCall)) {
                case "Portrait" -> Optional.of(SpeakerVisualRef.portrait(raw.orElseThrow()));
                case "SkinHead" -> Optional.of(SpeakerVisualRef.skinHead(raw.orElseThrow()));
                default -> {
                    diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-031",
                            "Visual intrinsic is not supported: " + CallSyntax.name(visualCall), visualCall));
                    yield Optional.empty();
                }
            };
        } catch (IllegalArgumentException exception) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-032", exception.getMessage(), visualCall));
            return Optional.empty();
        }
    }

    private Optional<SpeakerId> safeSpeakerId(String raw, List<DialogueDiagnostic> diagnostics) {
        try {
            return Optional.of(SpeakerId.parse(raw));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-016", exception.getMessage()));
            return Optional.empty();
        }
    }

    public record SpeakerBinding(SpeakerId speakerId, String displayName, Optional<SpeakerNameColor> nameColor,
            Optional<SpeakerVisualRef> visual, boolean displayOverride, boolean nameColorOverride,
            boolean visualOverride) {
        public SpeakerBinding {
            nameColor = Objects.requireNonNull(nameColor, "nameColor");
            visual = Objects.requireNonNull(visual, "visual");
        }
    }
}
