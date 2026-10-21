package io.github.kxyr0.echodialogue.echoscript.api;

import java.util.List;
import java.util.Objects;

/** Immutable result returned by a Line argument extension. */
public record DialogueLineArgumentBinding(
        List<DialogueLineExtensionCue> cues,
        List<DialogueLineBindingIssue> issues
) {
    public DialogueLineArgumentBinding {
        cues = List.copyOf(Objects.requireNonNull(cues, "cues"));
        issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
    }

    public static DialogueLineArgumentBinding accepted(List<DialogueLineExtensionCue> cues) {
        return new DialogueLineArgumentBinding(cues, List.of());
    }

    public static DialogueLineArgumentBinding rejected(String code, String message) {
        return new DialogueLineArgumentBinding(List.of(), List.of(DialogueLineBindingIssue.error(code, message)));
    }
}
