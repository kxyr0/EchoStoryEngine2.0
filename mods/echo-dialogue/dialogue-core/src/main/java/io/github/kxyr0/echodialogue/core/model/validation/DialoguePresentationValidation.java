package io.github.kxyr0.echodialogue.core.model.validation;

import io.github.kxyr0.echodialogue.core.model.DialoguePresentation;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import java.util.List;
import java.util.Objects;

/**
 * Validation shared by the compact dialogue presentation record.
 */
public final class DialoguePresentationValidation {
    private DialoguePresentationValidation() {
    }

    public static void validate(
            int revision,
            DialoguePresentationKind kind,
            List<DialoguePresentation.VisibleChoice> choices
    ) {
        if (revision < 0) {
            throw new IllegalArgumentException("Revision must not be negative.");
        }
        if (kind == DialoguePresentationKind.LINE && !choices.isEmpty()) {
            throw new IllegalArgumentException("LINE presentation cannot contain choices.");
        }
        if (kind == DialoguePresentationKind.CHOICE && choices.isEmpty()) {
            throw new IllegalArgumentException("CHOICE presentation must contain choices.");
        }
    }

    public static void validateSpeakerContext(String displayName, String text, boolean hasPresentation) {
        if (hasPresentation && (displayName.isBlank() || text.isBlank())) {
            throw new IllegalArgumentException("Speaker presentation requires speaker context.");
        }
    }

    public static String lineText(String value, DialoguePresentationKind kind, String name) {
        String checked = Objects.requireNonNull(value, name);
        if (kind == DialoguePresentationKind.CHOICE) {
            return checked.trim();
        }
        String trimmed = checked.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return trimmed;
    }
}
