package io.github.kxyr0.echoanimation.echoscript.face;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceControlId;
import io.github.kxyr0.echoanimation.api.id.FaceLayerId;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.id.FaceTextureId;
import io.github.kxyr0.echoanimation.api.math.IntRect;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec2;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.LayerRole;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneDefinition;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneHierarchy;
import io.github.kxyr0.echoanimation.definition.fingerprint.SemanticFingerprinter;
import io.github.kxyr0.echoanimation.definition.resource.FaceControlDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FaceLayerDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import io.github.kxyr0.echoanimation.definition.validation.FaceRigValidator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * Lowers high-level face rig syntax to immutable FaceRigDefinition.
 */
final class FaceRigLowerer {
    private static final Vec2 DEFAULT_TRAVEL = new Vec2(1000.0D, 1000.0D);
    private static final Vec2 DEFAULT_GAZE = new Vec2(1000.0D, 500.0D);

    private final FaceRigConventions conventions = new FaceRigConventions();

    FaceRigDefinition lower(FaceScriptModel.Face face) {
        State state = new State(face);
        addAutoBone(state, FaceRigConventions.FACE_ROOT, Optional.empty(), Transform3.IDENTITY);
        face.parts.forEach(part -> addPart(state, part));
        face.bones.forEach(bone -> addExtraBone(state, bone));
        face.blinks.forEach(blink -> state.controls.add(control(blink)));
        List<BoneDefinition> bones = List.copyOf(state.bones.values());
        FaceRigDefinition rig = new FaceRigDefinition(FaceRigId.parse(face.id), new BoneHierarchy(bones),
                List.copyOf(state.layers.values()), List.copyOf(state.controls),
                fingerprint(face.id, bones, state.layers.values(), state.controls));
        new FaceRigValidator().validate(rig);
        return rig;
    }

    private void addPart(State state, FaceScriptModel.Part part) {
        switch (part.kind()) {
            case SCLERA -> addEyeSurface(state, part, "eye_root", "sclera");
            case PUPIL -> addEyeSurface(state, part, "sclera", "pupil");
            case BROW -> addBrow(state, part);
            case EYELID -> addEyelidPair(state, part);
        }
    }

    private void addEyeSurface(State state, FaceScriptModel.Part part, String parentSuffix, String ownSuffix) {
        String side = part.variant().toLowerCase(java.util.Locale.ROOT);
        String eye = side + "_eye_root";
        addAutoBone(state, eye, Optional.of(FaceRigConventions.FACE_ROOT), Transform3.IDENTITY);
        String parent = "eye_root".equals(parentSuffix) ? eye : side + "_" + parentSuffix;
        if (!eye.equals(parent)) {
            addAutoBone(state, parent, Optional.of(eye), Transform3.IDENTITY);
        }
        Vec3 position = localPosition(state, Optional.of(parent), conventions.position(part));
        addAutoBone(state, side + "_" + ownSuffix, Optional.of(parent), transform(position, pivot(part.settings())));
        addLayer(state, new LayerDraft(conventions.layerId(part), conventions.boneId(part),
                conventions.texture(state.textureRoot, part), conventions.role(part), conventions.order(part),
                Transform3.IDENTITY, conventions.rect(part)));
    }

    private void addBrow(State state, FaceScriptModel.Part part) {
        String bone = conventions.boneId(part);
        addAutoBone(state, bone, Optional.of(FaceRigConventions.FACE_ROOT),
                transform(conventions.position(part), pivot(part.settings())));
        addLayer(state, new LayerDraft(conventions.layerId(part), bone, conventions.texture(state.textureRoot, part),
                conventions.role(part), conventions.order(part), Transform3.IDENTITY, conventions.rect(part)));
    }

    private void addEyelidPair(State state, FaceScriptModel.Part part) {
        String parent = conventions.boneId(part);
        addAutoBone(state, parent, Optional.of(FaceRigConventions.FACE_ROOT),
                transform(conventions.position(part), pivot(part.settings())));
        addSplitEyelid(state, part, parent, "left");
        addSplitEyelid(state, part, parent, "right");
    }

    private void addSplitEyelid(State state, FaceScriptModel.Part part, String parent, String side) {
        String bone = part.variant().toLowerCase(java.util.Locale.ROOT) + "_eyelid_" + side;
        Vec3 position = localPosition(state, Optional.of(parent), conventions.eyelidPosition(part, side));
        addAutoBone(state, bone, Optional.of(parent), transform(position, Vec3.ZERO));
        addLayer(state, new LayerDraft(conventions.splitEyelidLayerId(part.variant(), side), bone,
                conventions.texture(state.textureRoot, part), conventions.role(part),
                conventions.order(part) + ("right".equals(side) ? 1 : 0), Transform3.IDENTITY,
                conventions.eyelidRect(part, side)));
    }

    private void addExtraBone(State state, FaceScriptModel.ExtraBone bone) {
        String id = conventions.normalize(bone.name());
        addCustomBone(state, id, bone.parent().or(() -> Optional.of(FaceRigConventions.FACE_ROOT)),
                transform(bone.settings().position.orElse(Vec3.ZERO), pivot(bone.settings())));
        if ("glasses".equals(id)) {
            addGlassesLayer(state, id, bone.settings());
        }
    }

    private void addGlassesLayer(State state, String bone, FaceScriptModel.Settings settings) {
        addLayer(state, new LayerDraft("glasses", bone, conventions.glassesTexture(state.textureRoot, settings),
                settings.role.orElse(LayerRole.GLASSES), conventions.glassesOrder(settings),
                Transform3.IDENTITY, conventions.glassesRect(settings)));
    }

    private FaceControlDefinition control(FaceScriptModel.Blink blink) {
        Vec2 travel = blink.travel.orElse(DEFAULT_TRAVEL);
        Vec2 gaze = blink.gaze.orElse(DEFAULT_GAZE);
        FaceScriptModel.Timing timing = blink.timing.orElse(new FaceScriptModel.Timing(80, 3, 2, 4));
        return new FaceControlDefinition(FaceControlId.parse(blink.id), milli(travel.x()), milli(travel.y()),
                new Vec2(-milli(gaze.x()), -milli(gaze.y())),
                new Vec2(milli(gaze.x()), milli(gaze.y())),
                BoneId.parse("upper_eyelids"), BoneId.parse("lower_eyelids"),
                blink.idle.orElse(true), timing.periodTicks(), timing.closeTicks(),
                timing.holdTicks(), timing.openTicks());
    }

    private void addAutoBone(State state, String id, Optional<String> parent, Transform3 transform) {
        state.bones.putIfAbsent(id, new BoneDefinition(BoneId.parse(id), parent.map(BoneId::parse), transform));
    }

    private void addCustomBone(State state, String id, Optional<String> parent, Transform3 transform) {
        if (state.bones.containsKey(id)) {
            throw new IllegalArgumentException("Bone '" + id + "' was generated automatically or declared twice.");
        }
        state.bones.put(id, new BoneDefinition(BoneId.parse(id), parent.map(BoneId::parse), transform));
    }

    private void addLayer(State state, LayerDraft draft) {
        FaceLayerId id = FaceLayerId.parse(draft.id);
        FaceLayerDefinition layer = new FaceLayerDefinition(id, BoneId.parse(draft.bone),
                FaceTextureId.parse(draft.texture), draft.role, draft.order, draft.placement,
                draft.rect, Optional.empty(), true);
        if (state.layers.putIfAbsent(draft.id, layer) != null) {
            throw new IllegalArgumentException("Duplicate face element declaration: " + draft.id);
        }
    }

    private Transform3 transform(Vec3 position, Vec3 pivot) {
        return new Transform3(position, 0.0D, Vec3.ONE, pivot);
    }

    private Vec3 localPosition(State state, Optional<String> parent, Vec3 absolute) {
        return parent.map(value -> absolute.minus(worldPosition(state, value))).orElse(absolute);
    }

    private Vec3 worldPosition(State state, String bone) {
        BoneDefinition definition = state.bones.get(bone);
        Vec3 local = definition.localTransform().translation();
        return definition.parentId().map(parent -> worldPosition(state, parent.value()).plus(local)).orElse(local);
    }

    private double milli(double value) {
        return value / 1000.0D;
    }

    private Vec3 pivot(FaceScriptModel.Settings settings) {
        Vec3 value = settings.pivot.orElse(Vec3.ZERO);
        return new Vec3(milli(value.x()), milli(value.y()), milli(value.z()));
    }

    private String fingerprint(
            String id,
            List<BoneDefinition> bones,
            Iterable<FaceLayerDefinition> layers,
            List<FaceControlDefinition> controls
    ) {
        List<String> fields = new ArrayList<>();
        fields.add("id=" + id);
        bones.forEach(bone -> fields.add("bone=" + bone));
        layers.forEach(layer -> fields.add("layer=" + layer));
        controls.forEach(control -> fields.add("control=" + control));
        return new SemanticFingerprinter().fingerprint(fields);
    }

    private record LayerDraft(
            String id,
            String bone,
            String texture,
            LayerRole role,
            int order,
            Transform3 placement,
            IntRect rect
    ) {
    }

    private final class State {
        final String textureRoot;
        final LinkedHashMap<String, BoneDefinition> bones = new LinkedHashMap<>();
        final LinkedHashMap<String, FaceLayerDefinition> layers = new LinkedHashMap<>();
        final List<FaceControlDefinition> controls = new ArrayList<>();

        State(FaceScriptModel.Face face) {
            textureRoot = face.textureRoot.orElseGet(() -> conventions.defaultTextureRoot(face.id));
        }
    }
}
