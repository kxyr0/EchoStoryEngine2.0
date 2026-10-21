package io.github.kxyr0.echodialogue.core.fingerprint;

import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueChoice;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * Computes fingerprints from canonical compiled dialogue meaning.
 */
public final class CompiledDialogueFingerprinter {
    private CompiledDialogueFingerprinter() {
    }

    public static CompiledDialogueFingerprint fingerprint(CompiledDialogue dialogue) {
        return new CompiledDialogueFingerprint(HexFormat.of().formatHex(sha256(canonical(dialogue))));
    }

    private static byte[] sha256(String canonical) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 недоступен в JVM.", exception);
        }
    }

    private static String canonical(CompiledDialogue dialogue) {
        StringBuilder builder = new StringBuilder();
        append(builder, "dialogue", dialogue.id().toString());
        append(builder, "start", dialogue.startNodeId().toString());
        for (CompiledDialogueNode node : dialogue.nodes()) {
            appendNode(builder, node);
            for (CompiledDialogueChoice choice : node.choices()) {
                append(builder, "choice.id", choice.id().toString());
                append(builder, "choice.text", choice.text());
                append(builder, "choice.target", choice.targetNodeId().toString());
            }
            node.completion().ifPresent(completion ->
                    append(builder, "complete", completion.outcomeId().toString()));
            node.transition().ifPresent(transition -> {
                transition.targetNodeId().ifPresent(target -> append(builder, "transition.node", target.toString()));
                transition.targetDialogueId().ifPresent(target ->
                        append(builder, "transition.dialogue", target.toString()));
            });
        }
        return builder.toString();
    }

    private static void appendNode(StringBuilder builder, CompiledDialogueNode node) {
        append(builder, "node", node.id().toString());
        node.commands().forEach(command -> {
            append(builder, "command.type", command.type());
            append(builder, "command.position", Integer.toString(command.position()));
            command.arguments().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(argument -> {
                append(builder, "command.arg.name", argument.getKey());
                append(builder, "command.arg.value", argument.getValue());
            });
        });
        node.lines().forEach(line -> appendLine(builder, line));
    }

    private static void appendLine(StringBuilder builder, CompiledDialogueLine line) {
        append(builder, "line.ordinal", Integer.toString(line.lineIndex()));
        append(builder, "line.speaker", line.speakerId().toString());
        append(builder, "line.display", line.speakerDisplayName());
        line.speakerNameColor().ifPresent(color -> append(builder, "line.nameColor", color.hex()));
        append(builder, "line.visual.present", Boolean.toString(line.speakerVisual().isPresent()));
        line.speakerVisual().ifPresent(visual -> {
            append(builder, "line.visual.kind", visual.kind().name());
            append(builder, "line.visual.id", visual.id().toString());
        });
        append(builder, "line.text", line.text());
        append(builder, "line.key.present", Boolean.toString(line.key().isPresent()));
        line.key().ifPresent(key -> append(builder, "line.key", key));
        append(builder, "line.thought", Boolean.toString(line.thought()));
        for (CompiledDialogueLine.Cue cue : line.cues()) {
            append(builder, "line.cue.name", cue.name());
            append(builder, "line.cue.value", cue.value());
        }
    }

    private static void append(StringBuilder builder, String field, String value) {
        builder.append(field.length()).append(':').append(field)
                .append('=')
                .append(value.length()).append(':').append(value)
                .append('\n');
    }
}
