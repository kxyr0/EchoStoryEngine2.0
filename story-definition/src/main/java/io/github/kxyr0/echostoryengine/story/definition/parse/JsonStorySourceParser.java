package io.github.kxyr0.echostoryengine.story.definition.parse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.diagnostic.SourceDiagnosticCatalog;
import io.github.kxyr0.echostoryengine.story.definition.diagnostic.SourceDiagnosticContext;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceStory;
import io.github.kxyr0.echostoryengine.story.definition.source.StorySourceInput;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strict JSON source adapter for M2 story definitions.
 */
public final class JsonStorySourceParser {
    private static final Pattern INTEGER = Pattern.compile("\\d+");
    private static final Pattern LINE_COLUMN = Pattern.compile("line (\\d+) column (\\d+)");
    private final SourceDiagnosticCatalog diagnostics = new SourceDiagnosticCatalog();

    public StoryParseResult parse(StorySourceInput input, Instant occurredAt) {
        try {
            JsonElement root = JsonParser.parseString(input.content());
            SourceStory source = root.isJsonObject()
                    ? readStory(input.sourceId(), root.getAsJsonObject())
                    : emptyStory(input.sourceId());
            return new StoryParseResult(
                    input.sourceId(),
                    source.formatVersion(),
                    Optional.of(source),
                    List.of());
        } catch (JsonParseException exception) {
            SourceLocation location = syntaxLocation(exception.getMessage());
            String detail = exception.getMessage() == null ? "malformed JSON" : exception.getMessage();
            StoryDiagnostic diagnostic = diagnostics.create(
                    "ESE-SOURCE-001",
                    occurredAt,
                    SourceDiagnosticContext.fromInput(input, location, Map.of("detail", detail)));
            return new StoryParseResult(input.sourceId(), Optional.empty(), Optional.empty(), List.of(diagnostic));
        }
    }

    private SourceStory readStory(String sourceId, JsonObject root) {
        return new SourceStory(
                sourceId,
                optionalInteger(root, "formatVersion"),
                optionalString(root, "storyId"),
                optionalString(root, "startNode"),
                readNodes(root),
                root.keySet(),
                SourceLocation.unknown());
    }

    private List<SourceNode> readNodes(JsonObject root) {
        if (!root.has("nodes") || !root.get("nodes").isJsonArray()) {
            return List.of();
        }
        List<SourceNode> nodes = new ArrayList<>();
        root.getAsJsonArray("nodes").forEach(element -> nodes.add(readNode(element)));
        return List.copyOf(nodes);
    }

    private SourceNode readNode(JsonElement element) {
        if (!element.isJsonObject()) {
            return emptyNode();
        }
        JsonObject node = element.getAsJsonObject();
        ArgumentRead arguments = readArguments(node);
        return new SourceNode(
                optionalString(node, "id"),
                optionalString(node, "kind"),
                optionalString(node, "next"),
                optionalString(node, "variable"),
                optionalString(node, "value"),
                optionalString(node, "expectedValue"),
                optionalString(node, "actionType"),
                arguments.values(),
                arguments.invalidFields(),
                node.keySet(),
                SourceLocation.unknown());
    }

    private ArgumentRead readArguments(JsonObject node) {
        if (!node.has("arguments")) {
            return new ArgumentRead(Map.of(), List.of());
        }
        if (!node.get("arguments").isJsonObject()) {
            return new ArgumentRead(Map.of(), List.of("arguments"));
        }
        return readArgumentObject(node.getAsJsonObject("arguments"));
    }

    private ArgumentRead readArgumentObject(JsonObject arguments) {
        Map<String, String> values = new java.util.LinkedHashMap<>();
        List<String> invalid = new ArrayList<>();
        arguments.entrySet().forEach(entry -> {
            if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                values.put(entry.getKey(), entry.getValue().getAsString());
            } else {
                invalid.add(entry.getKey());
            }
        });
        return new ArgumentRead(values, invalid);
    }

    private Optional<String> optionalString(JsonObject object, String field) {
        if (!object.has(field) || !object.get(field).isJsonPrimitive()) {
            return Optional.empty();
        }
        JsonElement value = object.get(field);
        return value.getAsJsonPrimitive().isString() ? Optional.of(value.getAsString()) : Optional.empty();
    }

    private Optional<Integer> optionalInteger(JsonObject object, String field) {
        if (!object.has(field) || !object.get(field).isJsonPrimitive()) {
            return Optional.empty();
        }
        JsonElement value = object.get(field);
        String raw = value.getAsJsonPrimitive().getAsString();
        if (!value.getAsJsonPrimitive().isNumber() || !INTEGER.matcher(raw).matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(raw));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private SourceStory emptyStory(String sourceId) {
        return new SourceStory(
                sourceId,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Set.of(),
                SourceLocation.unknown());
    }

    private SourceNode emptyNode() {
        return new SourceNode(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Map.of(),
                List.of(),
                Set.of(),
                SourceLocation.unknown());
    }

    private SourceLocation syntaxLocation(String message) {
        Matcher matcher = LINE_COLUMN.matcher(message == null ? "" : message);
        if (!matcher.find()) {
            return SourceLocation.unknown();
        }
        return SourceLocation.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    }

    private record ArgumentRead(Map<String, String> values, List<String> invalidFields) {
        private ArgumentRead {
            values = Map.copyOf(values);
            invalidFields = List.copyOf(invalidFields);
        }
    }
}
