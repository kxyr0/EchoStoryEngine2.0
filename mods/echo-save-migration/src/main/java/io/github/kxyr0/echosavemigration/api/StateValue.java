package io.github.kxyr0.echosavemigration.api;

import java.util.Objects;

public sealed interface StateValue permits StateValue.BoolState, StateValue.IntState,
        StateValue.StringState, StateValue.ResourceState, StateValue.NodeState {
    record BoolState(boolean value) implements StateValue { }
    record IntState(int value) implements StateValue { }

    record StringState(String value) implements StateValue {
        public StringState { value = Objects.requireNonNull(value, "value"); }
    }

    record ResourceState(ResourceId value) implements StateValue {
        public ResourceState { value = Objects.requireNonNull(value, "value"); }
    }

    record NodeState(NodeId value) implements StateValue {
        public NodeState { value = Objects.requireNonNull(value, "value"); }
    }
}
