package io.github.kxyr0.echosavemigration.definition;

import io.github.kxyr0.echosavemigration.api.NodeId;
import io.github.kxyr0.echosavemigration.api.ResourceId;
import io.github.kxyr0.echosavemigration.api.StateValue;
import io.github.kxyr0.echosavemigration.data.MutableSaveCandidate;
import java.util.Map;
import java.util.Objects;

public sealed interface MigrationOperation permits MigrationOperation.RenameResource,
        MigrationOperation.RenameDialogueNode, MigrationOperation.RenameSceneNode,
        MigrationOperation.RenameState, MigrationOperation.RemoveState, MigrationOperation.CopyState,
        MigrationOperation.MoveState, MigrationOperation.SetDefaultIfMissing, MigrationOperation.ConvertState {
    void apply(MutableSaveCandidate candidate);

    record RenameResource(ResourceId oldId, ResourceId newId) implements MigrationOperation {
        public void apply(MutableSaveCandidate candidate) { candidate.renameResource(oldId, newId); }
    }

    record RenameDialogueNode(ResourceId resource, NodeId oldId, NodeId newId) implements MigrationOperation {
        public void apply(MutableSaveCandidate candidate) { candidate.renameDialogueNode(resource, oldId, newId); }
    }

    record RenameSceneNode(ResourceId resource, NodeId oldId, NodeId newId) implements MigrationOperation {
        public void apply(MutableSaveCandidate candidate) { candidate.renameSceneNode(resource, oldId, newId); }
    }

    record RenameState(ResourceId oldId, ResourceId newId) implements MigrationOperation {
        public void apply(MutableSaveCandidate candidate) { new MoveState(oldId, newId).apply(candidate); }
    }

    record RemoveState(ResourceId id) implements MigrationOperation {
        public void apply(MutableSaveCandidate candidate) { candidate.removeState(id); }
    }

    record CopyState(ResourceId from, ResourceId to) implements MigrationOperation {
        public void apply(MutableSaveCandidate candidate) { candidate.state(from).ifPresent(value -> candidate.state(to, value)); }
    }

    record MoveState(ResourceId from, ResourceId to) implements MigrationOperation {
        public void apply(MutableSaveCandidate candidate) {
            candidate.state(from).ifPresent(value -> { candidate.state(to, value); candidate.removeState(from); });
        }
    }

    record SetDefaultIfMissing(ResourceId id, StateValue value) implements MigrationOperation {
        public void apply(MutableSaveCandidate candidate) {
            if (candidate.state(id).isEmpty()) candidate.state(id, value);
        }
    }

    record ConvertState(ResourceId id, Map<String, String> canonicalValues) implements MigrationOperation {
        public ConvertState { canonicalValues = Map.copyOf(Objects.requireNonNull(canonicalValues, "canonicalValues")); }

        public void apply(MutableSaveCandidate candidate) {
            candidate.state(id).ifPresent(value -> candidate.state(id, convert(value)));
        }

        private StateValue convert(StateValue value) {
            String source;
            if (value instanceof StateValue.IntState integer) source = Integer.toString(integer.value());
            else if (value instanceof StateValue.BoolState bool) source = Boolean.toString(bool.value());
            else if (value instanceof StateValue.StringState text) source = text.value();
            else throw new IllegalArgumentException("Unsupported state conversion source.");
            String target = canonicalValues.get(source);
            if (target == null) throw new IllegalArgumentException("Missing controlled conversion for " + source);
            if (target.equals("true") || target.equals("false")) return new StateValue.BoolState(Boolean.parseBoolean(target));
            if (target.matches("-?\\d+")) return new StateValue.IntState(Integer.parseInt(target));
            return new StateValue.StringState(target);
        }
    }
}
