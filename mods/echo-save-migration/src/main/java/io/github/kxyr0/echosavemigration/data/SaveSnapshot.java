package io.github.kxyr0.echosavemigration.data;

import io.github.kxyr0.echosavemigration.api.EchoSaveManifest;
import io.github.kxyr0.echosavemigration.api.MigrationReceipt;
import io.github.kxyr0.echosavemigration.api.NodeId;
import io.github.kxyr0.echosavemigration.api.ResourceId;
import io.github.kxyr0.echosavemigration.api.StateValue;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record SaveSnapshot(
        EchoSaveManifest manifest,
        Map<String, ResourceId> resourceReferences,
        Map<ResourceId, NodeId> dialogueNodes,
        Map<ResourceId, NodeId> sceneNodes,
        Map<ResourceId, StateValue> states,
        List<MigrationReceipt> receipts
) {
    public SaveSnapshot {
        manifest = Objects.requireNonNull(manifest, "manifest");
        resourceReferences = Map.copyOf(resourceReferences);
        dialogueNodes = Map.copyOf(dialogueNodes);
        sceneNodes = Map.copyOf(sceneNodes);
        states = Map.copyOf(states);
        receipts = List.copyOf(receipts);
    }
}
