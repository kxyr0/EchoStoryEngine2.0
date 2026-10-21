package io.github.kxyr0.echostoryengine.story.persistence.schema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Optional;

final class SnapshotJsonWriter {
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

    String write(ExecutionSnapshot snapshot) {
        JsonObject object = new JsonObject();
        object.addProperty("saveSchemaVersion", snapshot.saveSchemaVersion());
        object.addProperty("storyId", snapshot.storyId());
        object.addProperty("executionId", snapshot.executionId());
        object.addProperty("definitionFingerprint", snapshot.definitionFingerprint());
        object.addProperty("definitionRevision", snapshot.definitionRevision());
        object.addProperty("executionRevision", snapshot.executionRevision());
        object.addProperty("phase", snapshot.phase());
        object.addProperty("currentNode", snapshot.currentNode());
        object.add("variables", map(snapshot.variables()));
        object.addProperty("pendingActionExecutionId", snapshot.pendingActionExecutionId());
        object.add("pendingAction", pending(snapshot.pendingAction()));
        object.addProperty("lastCompletedActionExecutionId", snapshot.lastCompletedActionExecutionId());
        object.addProperty("failureCode", snapshot.failureCode());
        object.addProperty("createdAt", snapshot.createdAt());
        object.addProperty("updatedAt", snapshot.updatedAt());
        return gson.toJson(object);
    }

    private JsonObject map(Map<String, String> values) {
        JsonObject object = new JsonObject();
        values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> object.addProperty(entry.getKey(), entry.getValue()));
        return object;
    }

    private com.google.gson.JsonElement pending(Optional<PendingActionSnapshot> pending) {
        if (pending.isEmpty()) {
            return com.google.gson.JsonNull.INSTANCE;
        }
        PendingActionSnapshot snapshot = pending.orElseThrow();
        JsonObject object = new JsonObject();
        object.addProperty("actionExecutionId", snapshot.actionExecutionId());
        object.addProperty("actionType", snapshot.actionType());
        object.add("arguments", map(snapshot.arguments()));
        return object;
    }
}
