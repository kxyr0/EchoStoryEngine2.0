package io.github.kxyr0.echocontentdoctor.validation.structural;

import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.graph.ContentGraph;
import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentReferenceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SceneRecursionValidator {
    public List<ContentDiagnostic> validate(ContentGraph graph) {
        Map<ContentResourceId, ContentDescriptor> scenes = new HashMap<>();
        graph.snapshot().resources().stream().filter(value -> value.kind() == ContentResourceKind.SCENE)
                .forEach(value -> scenes.putIfAbsent(value.id(), value));
        List<ContentDiagnostic> diagnostics = new ArrayList<>();
        for (ContentDescriptor scene : scenes.values()) {
            if (cycle(scene.id(), scene.id(), scenes, new HashSet<>())) diagnostics.add(new ContentDiagnostic(
                    ContentSeverity.ERROR, "ECHO_DOCTOR_SCENE_RECURSION", scene.id(), scene.span(),
                    "Scene recursion has no statically visible exit path.", List.of()));
        }
        return diagnostics;
    }

    private boolean cycle(ContentResourceId root, ContentResourceId current,
            Map<ContentResourceId, ContentDescriptor> scenes, Set<ContentResourceId> path) {
        if (!path.add(current)) return current.equals(root);
        ContentDescriptor descriptor = scenes.get(current);
        if (descriptor == null) return false;
        boolean found = descriptor.references().stream().filter(value -> value.kind() == ContentReferenceKind.SCENE)
                .anyMatch(value -> cycle(root, value.target(), scenes, new HashSet<>(path)));
        return found;
    }
}
