package io.github.kxyr0.echonpc.persistence.schema;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Optional;

/**
 * Deterministic UTF-8 JSON codec for Actor Save Schema V1-V4.
 */
public final class ActorSaveCodec {
    private static final Gson GSON = new Gson();

    public String encode(ActorSaveRecord record) {
        StringBuilder out = new StringBuilder(512);
        out.append("{\n");
        line(out, "schemaVersion", Integer.toString(record.schemaVersion()), true);
        line(out, "actorId", quote(record.actorId()), true);
        line(out, "definitionId", quote(record.definitionId()), true);
        line(out, "definitionFingerprint", quote(record.definitionFingerprint()), true);
        line(out, "presence", quote(record.presence()), true);
        line(out, "worldId", quote(record.worldId()), true);
        out.append("  \"position\": {\"x\": ").append(record.x()).append(", \"y\": ").append(record.y())
                .append(", \"z\": ").append(record.z()).append("},\n");
        out.append("  \"rotation\": {\"yaw\": ").append(record.yaw()).append(", \"pitch\": ")
                .append(record.pitch()).append("},\n");
        line(out, "entityBindingId", quote(record.entityBindingId()), true);
        line(out, "actorRevision", Long.toString(record.actorRevision()), true);
        line(out, "repositoryRevision", Long.toString(record.repositoryRevision()), true);
        line(out, "createdAtEpochMillis", Long.toString(record.createdAtEpochMillis()), true);
        boolean hasTail = record.activeSkinId().isPresent() || record.activeForm().isPresent()
                || record.restAttachment().isPresent();
        line(out, "updatedAtEpochMillis", Long.toString(record.updatedAtEpochMillis()), hasTail);
        record.activeSkinId().ifPresent(skin -> line(out, "activeSkinId", quote(skin),
                record.activeForm().isPresent() || record.restAttachment().isPresent()));
        record.activeForm().ifPresent(form -> appendForm(out, form, record.restAttachment().isPresent()));
        record.restAttachment().ifPresent(rest -> appendRest(out, rest));
        out.append("}\n");
        return out.toString();
    }

    public ActorSaveRecord decode(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        int schemaVersion = integer(root, "schemaVersion");
        if (schemaVersion < 1 || schemaVersion > 4) {
            throw new UnsupportedOperationException("Unsupported schemaVersion: " + schemaVersion);
        }
        JsonObject position = object(root, "position");
        JsonObject rotation = object(root, "rotation");
        Optional<FormSaveRecord> form = schemaVersion >= 3 && root.has("activeForm")
                ? Optional.of(formRecord(object(root, "activeForm"))) : Optional.empty();
        Optional<String> skin = activeSkin(root, schemaVersion);
        Optional<RestSaveRecord> rest = schemaVersion == 2 && root.has("restAttachment")
                ? Optional.of(restRecord(object(root, "restAttachment"))) : Optional.empty();
        rest = schemaVersion >= 3 && root.has("restAttachment")
                ? Optional.of(restRecord(object(root, "restAttachment"))) : rest;
        return new ActorSaveRecord(schemaVersion, string(root, "actorId"),
                string(root, "definitionId"), string(root, "definitionFingerprint"), string(root, "presence"),
                string(root, "worldId"), number(position, "x"), number(position, "y"), number(position, "z"),
                decimal(rotation, "yaw"), decimal(rotation, "pitch"), string(root, "entityBindingId"),
                longValue(root, "actorRevision"), longValue(root, "repositoryRevision"),
                longValue(root, "createdAtEpochMillis"), longValue(root, "updatedAtEpochMillis"), skin, form, rest);
    }

    private void appendForm(StringBuilder out, FormSaveRecord form, boolean comma) {
        out.append("  \"activeForm\": {\n");
        line(out, "formId", quote(form.formId()), true);
        line(out, "revision", Long.toString(form.revision()), true);
        line(out, "fingerprint", quote(form.fingerprint()), false);
        out.append("  }").append(comma ? ",\n" : "\n");
    }

    private void appendRest(StringBuilder out, RestSaveRecord rest) {
        out.append("  \"restAttachment\": {\n");
        line(out, "providerId", quote(rest.providerId()), true);
        line(out, "bedWorldId", quote(rest.bedWorldId()), true);
        out.append("    \"bed\": {\"x\": ").append(rest.bedX()).append(", \"y\": ").append(rest.bedY())
                .append(", \"z\": ").append(rest.bedZ()).append("},\n");
        line(out, "expectedBlockId", quote(rest.expectedBlockId()), true);
        out.append("    \"canonicalBed\": {\"x\": ").append(rest.canonicalBedX()).append(", \"y\": ")
                .append(rest.canonicalBedY()).append(", \"z\": ").append(rest.canonicalBedZ()).append("},\n");
        out.append("    \"sleepAnchor\": {\"x\": ").append(rest.anchorX()).append(", \"y\": ")
                .append(rest.anchorY()).append(", \"z\": ").append(rest.anchorZ()).append("},\n");
        line(out, "sleepYaw", Float.toString(rest.sleepYaw()), true);
        line(out, "pose", quote(rest.pose()), true);
        line(out, "attachmentRevision", Long.toString(rest.attachmentRevision()), true);
        line(out, "providerState", quote(rest.providerState()), true);
        line(out, "occupied", Boolean.toString(rest.occupied()), true);
        line(out, "commandId", quote(rest.commandId()), false);
        out.append("  }\n");
    }

    private RestSaveRecord restRecord(JsonObject object) {
        JsonObject bed = this.object(object, "bed");
        JsonObject canonicalBed = this.object(object, "canonicalBed");
        JsonObject sleepAnchor = this.object(object, "sleepAnchor");
        return new RestSaveRecord(string(object, "providerId"), string(object, "bedWorldId"),
                integer(bed, "x"), integer(bed, "y"), integer(bed, "z"),
                string(object, "expectedBlockId"), integer(canonicalBed, "x"), integer(canonicalBed, "y"),
                integer(canonicalBed, "z"), number(sleepAnchor, "x"), number(sleepAnchor, "y"),
                number(sleepAnchor, "z"), decimal(object, "sleepYaw"), string(object, "pose"),
                longValue(object, "attachmentRevision"), string(object, "providerState"),
                object.get("occupied").getAsBoolean(), string(object, "commandId"));
    }

    private FormSaveRecord formRecord(JsonObject object) {
        return new FormSaveRecord(string(object, "formId"), longValue(object, "revision"),
                string(object, "fingerprint"));
    }

    private Optional<String> activeSkin(JsonObject root, int schemaVersion) {
        return schemaVersion >= 4 && root.has("activeSkinId")
                ? Optional.of(string(root, "activeSkinId")) : Optional.empty();
    }

    private void line(StringBuilder out, String name, String value, boolean comma) {
        out.append("  \"").append(name).append("\": ").append(value);
        out.append(comma ? ",\n" : "\n");
    }

    private String quote(String value) {
        return GSON.toJson(value);
    }

    private JsonObject object(JsonObject root, String name) {
        if (!root.has(name) || !root.get(name).isJsonObject()) {
            throw new IllegalArgumentException("Missing JSON object: " + name);
        }
        return root.getAsJsonObject(name);
    }

    private String string(JsonObject root, String name) {
        if (!root.has(name) || !root.get(name).isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing JSON string: " + name);
        }
        return root.get(name).getAsString();
    }

    private int integer(JsonObject root, String name) {
        return root.get(name).getAsInt();
    }

    private long longValue(JsonObject root, String name) {
        return root.get(name).getAsLong();
    }

    private double number(JsonObject root, String name) {
        return root.get(name).getAsDouble();
    }

    private float decimal(JsonObject root, String name) {
        return root.get(name).getAsFloat();
    }
}
