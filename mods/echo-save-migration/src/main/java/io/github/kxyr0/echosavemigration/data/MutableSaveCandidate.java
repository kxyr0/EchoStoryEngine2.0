package io.github.kxyr0.echosavemigration.data;

import io.github.kxyr0.echosavemigration.api.EchoSaveManifest;
import io.github.kxyr0.echosavemigration.api.MigrationReceipt;
import io.github.kxyr0.echosavemigration.api.NodeId;
import io.github.kxyr0.echosavemigration.api.ResourceId;
import io.github.kxyr0.echosavemigration.api.StateValue;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MutableSaveCandidate {
    private EchoSaveManifest manifest;
    private final Map<String, ResourceId> resources;
    private final Map<ResourceId, NodeId> dialogueNodes;
    private final Map<ResourceId, NodeId> sceneNodes;
    private final Map<ResourceId, StateValue> states;
    private final List<MigrationReceipt> receipts;

    public MutableSaveCandidate(SaveSnapshot source) {
        manifest = source.manifest();
        resources = new LinkedHashMap<>(source.resourceReferences());
        dialogueNodes = new LinkedHashMap<>(source.dialogueNodes());
        sceneNodes = new LinkedHashMap<>(source.sceneNodes());
        states = new LinkedHashMap<>(source.states());
        receipts = new ArrayList<>(source.receipts());
    }

    public EchoSaveManifest manifest() { return manifest; }
    public void manifest(EchoSaveManifest value) { manifest = value; }
    public Optional<StateValue> state(ResourceId id) { return Optional.ofNullable(states.get(id)); }
    public void state(ResourceId id, StateValue value) { states.put(id, value); }
    public void removeState(ResourceId id) { states.remove(id); }
    public void receipt(MigrationReceipt value) { receipts.add(value); }

    public void renameResource(ResourceId oldId, ResourceId newId) {
        resources.replaceAll((slot, value) -> value.equals(oldId) ? newId : value);
        renameKey(dialogueNodes, oldId, newId);
        renameKey(sceneNodes, oldId, newId);
        states.replaceAll((key, value) -> value instanceof StateValue.ResourceState resource
                && resource.value().equals(oldId) ? new StateValue.ResourceState(newId) : value);
    }

    public void renameDialogueNode(ResourceId resource, NodeId oldId, NodeId newId) {
        dialogueNodes.computeIfPresent(resource, (ignored, current) -> current.equals(oldId) ? newId : current);
    }

    public void renameSceneNode(ResourceId resource, NodeId oldId, NodeId newId) {
        sceneNodes.computeIfPresent(resource, (ignored, current) -> current.equals(oldId) ? newId : current);
    }

    public SaveSnapshot freeze() {
        return new SaveSnapshot(manifest, resources, dialogueNodes, sceneNodes, states, receipts);
    }

    private <T> void renameKey(Map<ResourceId, T> values, ResourceId oldId, ResourceId newId) {
        T value = values.remove(oldId);
        if (value != null) {
            values.put(newId, value);
        }
    }
}
