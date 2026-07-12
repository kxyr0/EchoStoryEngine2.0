package io.github.kxyr0.echostoryengine.story.persistence.schema;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

final class SnapshotJsonReader {
    private static final Pattern INTEGER = Pattern.compile("0|[1-9][0-9]*");
    private static final Set<String> ROOT_FIELDS = Set.of(
            "saveSchemaVersion",
            "storyId",
            "executionId",
            "definitionFingerprint",
            "definitionRevision",
            "executionRevision",
            "phase",
            "currentNode",
            "variables",
            "pendingActionExecutionId",
            "pendingAction",
            "lastCompletedActionExecutionId",
            "failureCode",
            "createdAt",
            "updatedAt");
    private static final Set<String> PENDING_FIELDS = Set.of("actionExecutionId", "actionType", "arguments");

    ExecutionSnapshot read(String content) throws SnapshotDecodeException {
        try {
            JsonObject object = requireObject(JsonParser.parseString(content), "root");
            requireFields(object, ROOT_FIELDS, "root");
            int schemaVersion = readSchemaVersion(object);
            return snapshot(schemaVersion, object);
        } catch (SnapshotDecodeException exception) {
            throw exception;
        } catch (JsonParseException | IllegalArgumentException | IllegalStateException exception) {
            throw malformed("invalid JSON snapshot", exception);
        }
    }

    private ExecutionSnapshot snapshot(int schemaVersion, JsonObject object) throws SnapshotDecodeException {
        return new ExecutionSnapshot(
                schemaVersion,
                string(object, "storyId"),
                string(object, "executionId"),
                string(object, "definitionFingerprint"),
                string(object, "definitionRevision"),
                integer(object, "executionRevision"),
                string(object, "phase"),
                string(object, "currentNode"),
                map(object, "variables"),
                string(object, "pendingActionExecutionId"),
                pending(object.get("pendingAction")),
                string(object, "lastCompletedActionExecutionId"),
                string(object, "failureCode"),
                instant(object, "createdAt"),
                instant(object, "updatedAt"));
    }

    private int readSchemaVersion(JsonObject object) throws SnapshotDecodeException {
        int schemaVersion = integer(object, "saveSchemaVersion");
        if (schemaVersion != ExecutionSnapshot.SUPPORTED_SCHEMA_VERSION) {
            throw new SnapshotDecodeException("ESE-PERSISTENCE-005", "unsupported save schema version");
        }
        return schemaVersion;
    }

    private Optional<PendingActionSnapshot> pending(JsonElement element) throws SnapshotDecodeException {
        if (element == null || element.isJsonNull()) {
            return Optional.empty();
        }
        JsonObject object = requireObject(element, "pendingAction");
        requireFields(object, PENDING_FIELDS, "pendingAction");
        return Optional.of(new PendingActionSnapshot(
                string(object, "actionExecutionId"),
                string(object, "actionType"),
                map(object, "arguments")));
    }

    private Map<String, String> map(JsonObject parent, String field) throws SnapshotDecodeException {
        JsonObject object = requireObject(parent.get(field), field);
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        object.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> result.put(entry.getKey(), stringValue(entry.getValue(), field)));
        return result;
    }

    private void requireFields(JsonObject object, Set<String> fields, String label) throws SnapshotDecodeException {
        if (!object.keySet().equals(fields)) {
            throw malformed(label + " fields do not match schema");
        }
    }

    private JsonObject requireObject(JsonElement element, String label) throws SnapshotDecodeException {
        if (element == null || !element.isJsonObject()) {
            throw malformed(label + " must be an object");
        }
        return element.getAsJsonObject();
    }

    private String string(JsonObject object, String field) throws SnapshotDecodeException {
        return stringValue(object.get(field), field);
    }

    private String stringValue(JsonElement element, String label) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(label + " must be a string");
        }
        return element.getAsString();
    }

    private int integer(JsonObject object, String field) throws SnapshotDecodeException {
        try {
            JsonElement element = object.get(field);
            if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
                throw malformed(field + " must be an integer");
            }
            String value = element.getAsString();
            if (!INTEGER.matcher(value).matches()) {
                throw malformed(field + " must be an integer");
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException | IllegalStateException exception) {
            throw malformed(field + " must be an integer", exception);
        }
    }

    private String instant(JsonObject object, String field) throws SnapshotDecodeException {
        String value = string(object, field);
        try {
            Instant.parse(value);
            return value;
        } catch (DateTimeParseException exception) {
            throw malformed(field + " must be an ISO-8601 instant", exception);
        }
    }

    private SnapshotDecodeException malformed(String message) {
        return new SnapshotDecodeException("ESE-PERSISTENCE-004", message);
    }

    private SnapshotDecodeException malformed(String message, Throwable cause) {
        return new SnapshotDecodeException("ESE-PERSISTENCE-004", message, cause);
    }
}
