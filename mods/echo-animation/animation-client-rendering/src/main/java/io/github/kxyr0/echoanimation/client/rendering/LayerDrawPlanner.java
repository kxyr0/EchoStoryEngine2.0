package io.github.kxyr0.echoanimation.client.rendering;

import io.github.kxyr0.echoanimation.api.id.FaceLayerId;
import io.github.kxyr0.echoanimation.definition.resource.FaceLayerDefinition;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Produces deterministic face layer draw commands.
 */
public final class LayerDrawPlanner {
    public List<LayerDrawCommand> plan(List<FaceLayerDefinition> layers, Set<FaceLayerId> hiddenLayers) {
        return layers.stream()
                .filter(layer -> layer.visibleByDefault() && !hiddenLayers.contains(layer.id()))
                .sorted(ordering())
                .map(this::command)
                .toList();
    }

    private Comparator<FaceLayerDefinition> ordering() {
        return Comparator.comparingInt(FaceLayerDefinition::drawOrder)
                .thenComparing(layer -> layer.role().ordinal())
                .thenComparing(layer -> layer.id().value());
    }

    private LayerDrawCommand command(FaceLayerDefinition layer) {
        return new LayerDrawCommand(
                layer.id(),
                layer.boneId(),
                layer.textureId(),
                layer.role(),
                layer.placement(),
                layer.atlasRect(),
                layer.clipRect());
    }
}
