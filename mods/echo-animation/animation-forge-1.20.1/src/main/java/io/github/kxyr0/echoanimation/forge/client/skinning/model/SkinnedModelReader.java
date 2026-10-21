package io.github.kxyr0.echoanimation.forge.client.skinning.model;

import io.github.kxyr0.echoanimation.forge.client.skinning.model.weights.SkinWeights;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoanimation.forge.api.client.render.attachment.AnimationAttachmentTransformSpace;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.slf4j.Logger;

/** Reads compact JSON skin format v2 and adapts v1 rigid vertices. */
public final class SkinnedModelReader {
    private static final Logger LOGGER = LogUtils.getLogger();

    public SkinnedModel read(String source, Reader input, int boneLimit) {
        JsonObject root = JsonParser.parseReader(input).getAsJsonObject();
        int version = root.has("formatVersion") ? root.get("formatVersion").getAsInt() : 1;
        List<SkinnedBone> bones = readBones(requiredArray(root, "bones"), version);
        int rootBone = rootBone(bones);
        List<SkinnedMesh> meshes = readMeshes(source, requiredArray(root, "meshes"), version, rootBone);
        float[] renderOffset = root.has("renderOffset")
                ? floats(root.getAsJsonArray("renderOffset"), 3)
                : new float[] {0.0F, 0.0F, 0.0F};
        SkinnedAttachmentBindings attachmentBindings = root.has("attachmentBindings")
                ? readAttachmentBindings(root.getAsJsonObject("attachmentBindings"))
                : SkinnedAttachmentBindings.legacy();
        SkinnedModel model = new SkinnedModel(version, source, bones, meshes,
                renderOffset[0], renderOffset[1], renderOffset[2], attachmentBindings);
        new SkinnedModelValidator().validate(model, boneLimit);
        return model;
    }

    private SkinnedAttachmentBindings readAttachmentBindings(JsonObject value) {
        AnimationAttachmentTransformSpace space;
        try {
            space = AnimationAttachmentTransformSpace.valueOf(
                    requiredString(value, "space").trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unknown attachment binding space: " + value.get("space"),
                    exception);
        }
        return new SkinnedAttachmentBindings(
                space,
                requiredString(value, "torsoBack"),
                requiredString(value, "leftHand"),
                requiredString(value, "rightHand"),
                value.has("cancelBindBasis")
                        && value.get("cancelBindBasis").getAsBoolean(),
                true);
    }

    private List<SkinnedBone> readBones(JsonArray values, int version) {
        List<SkinnedBone> bones = new ArrayList<>();
        for (JsonElement value : values) {
            JsonObject bone = value.getAsJsonObject();
            Matrix4f bindLocal = matrix(bone, "bindLocal", version == 1);
            Matrix4f inverseBind = matrix(bone, "inverseBind", version == 1);
            bones.add(new SkinnedBone(requiredString(bone, "name"), requiredInt(bone, "parent"),
                    bindLocal, inverseBind));
        }
        return List.copyOf(bones);
    }

    private List<SkinnedMesh> readMeshes(
            String source,
            JsonArray values,
            int version,
            int rootBone
    ) {
        List<SkinnedMesh> meshes = new ArrayList<>();
        for (JsonElement value : values) {
            JsonObject mesh = value.getAsJsonObject();
            String name = requiredString(mesh, "name");
            int rigidBone = mesh.has("bone") ? mesh.get("bone").getAsInt() : rootBone;
            ReadContext context = new ReadContext(source, name, version, rigidBone, rootBone);
            List<SkinnedVertex> vertices = readVertices(requiredArray(mesh, "vertices"), context);
            JsonArray triangles = mesh.has("triangles") ? mesh.getAsJsonArray("triangles")
                    : requiredArray(mesh, "indices");
            meshes.add(new SkinnedMesh(name, vertices, ints(triangles)));
        }
        return List.copyOf(meshes);
    }

    private List<SkinnedVertex> readVertices(JsonArray values, ReadContext context) {
        List<SkinnedVertex> vertices = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            JsonObject vertex = values.get(index).getAsJsonObject();
            float[] position = floats(requiredArray(vertex, "position"), 3);
            float[] normal = floats(requiredArray(vertex, "normal"), 3);
            float[] uv = floats(requiredArray(vertex, "uv"), 2);
            SkinWeights skin = readSkin(vertex, context.version(), context.rigidBone(), context.rootBone());
            reportSkinRepair(context.source(), context.mesh(), index, skin);
            vertices.add(new SkinnedVertex(position[0], position[1], position[2],
                    normal[0], normal[1], normal[2], uv[0], uv[1], skin));
        }
        return List.copyOf(vertices);
    }

    private SkinWeights readSkin(JsonObject vertex, int version, int rigidBone, int rootBone) {
        if (version == 1 || !vertex.has("boneIndices") || !vertex.has("boneWeights")) {
            return SkinWeights.rigid(version == 1 ? rigidBone : rootBone, version != 1);
        }
        return SkinWeights.of(ints(vertex.getAsJsonArray("boneIndices")),
                floats(vertex.getAsJsonArray("boneWeights"), -1), rootBone);
    }

    private void reportSkinRepair(String source, String mesh, int vertex, SkinWeights skin) {
        if (skin.truncated()) {
            LOGGER.warn("Skinned model {} mesh {} vertex {}: kept four strongest influences", source, mesh, vertex);
        }
        if (skin.fallback()) {
            LOGGER.warn("Skinned model {} mesh {} vertex {}: attached to root bone", source, mesh, vertex);
        }
    }

    private Matrix4f matrix(JsonObject object, String name, boolean optional) {
        if (!object.has(name) && optional) {
            return new Matrix4f();
        }
        return new Matrix4f().set(floats(requiredArray(object, name), 16));
    }

    private int rootBone(List<SkinnedBone> bones) {
        for (int index = 0; index < bones.size(); index++) {
            if (bones.get(index).parentIndex() == -1) {
                return index;
            }
        }
        throw new IllegalArgumentException("Skinned model has no root bone.");
    }

    private JsonArray requiredArray(JsonObject object, String name) {
        if (!object.has(name) || !object.get(name).isJsonArray()) {
            throw new IllegalArgumentException("Required array is missing: " + name);
        }
        return object.getAsJsonArray(name);
    }

    private String requiredString(JsonObject object, String name) {
        if (!object.has(name)) {
            throw new IllegalArgumentException("Required string is missing: " + name);
        }
        return object.get(name).getAsString();
    }

    private int requiredInt(JsonObject object, String name) {
        if (!object.has(name)) {
            throw new IllegalArgumentException("Required integer is missing: " + name);
        }
        return object.get(name).getAsInt();
    }

    private int[] ints(JsonArray values) {
        int[] result = new int[values.size()];
        for (int index = 0; index < result.length; index++) {
            result[index] = values.get(index).getAsInt();
        }
        return result;
    }

    private float[] floats(JsonArray values, int expected) {
        if (expected >= 0 && values.size() != expected) {
            throw new IllegalArgumentException("Expected " + expected + " values, got " + values.size());
        }
        float[] result = new float[values.size()];
        for (int index = 0; index < result.length; index++) {
            result[index] = values.get(index).getAsFloat();
        }
        return result;
    }

    private record ReadContext(String source, String mesh, int version, int rigidBone, int rootBone) {
    }
}
