package io.github.kxyr0.echostoryengine.story.core.domain;

import io.github.kxyr0.echostoryengine.story.api.identity.CompiledStoryFingerprint;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

/**
 * Computes deterministic compatibility tokens from compiled story definitions.
 */
public final class StoryDefinitionFingerprint {
    private StoryDefinitionFingerprint() {
    }

    /**
     * Computes a canonical SHA-256 fingerprint from compiled story meaning.
     */
    public static CompiledStoryFingerprint from(StoryDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return new CompiledStoryFingerprint(hash(canonical(definition)));
    }

    private static String canonical(StoryDefinition definition) {
        StringBuilder builder = new StringBuilder();
        append(builder, "format", "compiled-story-v1");
        append(builder, "storyId", definition.storyId().asString());
        append(builder, "startNode", definition.startNode().value());
        definition.nodes().values().forEach(node -> appendNode(builder, node));
        return builder.toString();
    }

    private static void appendNode(StringBuilder builder, StoryNode node) {
        append(builder, "nodeId", node.id().value());
        append(builder, "kind", node.kind().name());
        append(builder, "nextNode", node.nextNode().value());
        append(builder, "variableName", node.variableName());
        append(builder, "variableValue", node.variableValue());
        append(builder, "actionType", node.actionType().asString());
        append(builder, "outputName", node.outputName());
        append(builder, "argumentCount", Integer.toString(node.actionArguments().size()));
        node.actionArguments().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> appendArgument(builder, entry));
    }

    private static void appendArgument(StringBuilder builder, Map.Entry<String, String> entry) {
        append(builder, "argumentKey", entry.getKey());
        append(builder, "argumentValue", entry.getValue());
    }

    private static void append(StringBuilder builder, String label, String value) {
        builder.append(label)
                .append('=')
                .append(value.length())
                .append(':')
                .append(value)
                .append(';');
    }

    private static String hash(String canonical) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
        }
    }
}
