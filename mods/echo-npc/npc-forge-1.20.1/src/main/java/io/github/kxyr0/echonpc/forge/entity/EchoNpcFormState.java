package io.github.kxyr0.echonpc.forge.entity;

import io.github.kxyr0.echonpc.api.form.body.NpcBodyAnchorDefinition;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyPartTransform;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormDefinition;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

final class EchoNpcFormState {
    static final int FORM_ID = 0;
    static final int REVISION = 1;
    static final int FINGERPRINT = 2;
    static final int MODEL = 3;
    static final int ANIMATION_GRAPH = 4;
    static final int FACE_PROFILE = 5;
    static final int PORTRAIT = 6;
    static final int PARTS = 7;
    static final int ANCHORS = 8;
    private static final String SEP = "|";

    private EchoNpcFormState() {
    }

    static String encode(NpcFormDefinition form) {
        return String.join(SEP, form.id().value(), Long.toString(form.revision().value()),
                form.fingerprint(), form.appearance().modelProfile(), form.appearance().animationGraphId(),
                form.appearance().faceProfileId(), form.appearance().portraitResourceId(),
                parts(form.bodyGeometry().parts()), anchors(form.anchors()));
    }

    static String field(String state, int index) {
        if (state == null || state.isBlank()) {
            return "";
        }
        String[] fields = state.split("\\|", -1);
        return index >= 0 && index < fields.length ? fields[index] : "";
    }

    static String parts(String state) {
        return field(state, PARTS);
    }

    static String anchors(String state) {
        return field(state, ANCHORS);
    }

    private static String parts(List<NpcBodyPartTransform> parts) {
        return parts.stream().map(part -> part.partId().name().toLowerCase(java.util.Locale.ROOT)
                + "," + part.scaleX() + "," + part.scaleY() + "," + part.scaleZ()
                + "," + part.pivotX() + "," + part.pivotY() + "," + part.pivotZ()
                + "," + part.rotationX() + "," + part.rotationY() + "," + part.rotationZ())
                .collect(Collectors.joining(";"));
    }

    private static String anchors(List<NpcBodyAnchorDefinition> anchors) {
        return anchors.stream().map(anchor -> anchor.id().value() + "," + anchor.x() + "," + anchor.y()
                + "," + anchor.z()).collect(Collectors.joining(";"));
    }

    static Optional<String> optionalField(String state, int index) {
        String value = field(state, index);
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }
}
