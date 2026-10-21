package io.github.kxyr0.echoanimation.api.graph.definition;

import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationGraphId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable shared graph definition; subjects own runtime state separately.
 */
public record AnimationGraphDefinition(
        AnimationGraphId id,
        long revision,
        List<AnimationChannelDefinition> channels,
        List<AnimationStateDefinition> states,
        List<AnimationTransitionDefinition> transitions
) {
    public AnimationGraphDefinition {
        id = Objects.requireNonNull(id, "id");
        channels = List.copyOf(Objects.requireNonNull(channels, "channels"));
        states = List.copyOf(Objects.requireNonNull(states, "states"));
        transitions = List.copyOf(Objects.requireNonNull(transitions, "transitions"));
        if (revision <= 0L) {
            throw new IllegalArgumentException("revision must be positive.");
        }
        requireUniqueChannels(channels);
        requireUniqueStates(states);
        requireInitialStates(channels, states);
        requireTransitionStates(transitions, states);
        requireUnambiguousOverrideLayers(channels);
    }

    public Optional<AnimationChannelDefinition> channel(AnimationChannelId channelId) {
        return channels.stream().filter(channel -> channel.id().equals(channelId)).findFirst();
    }

    public Optional<AnimationStateDefinition> state(AnimationStateId stateId) {
        return states.stream().filter(state -> state.id().equals(stateId)).findFirst();
    }

    public List<AnimationTransitionDefinition> transitionsFor(
            AnimationChannelId channelId,
            AnimationStateId stateId,
            String eventId
    ) {
        return transitions.stream()
                .filter(transition -> transition.channelId().equals(channelId))
                .filter(transition -> transition.fromStateId().equals(stateId))
                .filter(transition -> transition.eventId().equals(eventId))
                .sorted(Comparator.comparingInt(AnimationTransitionDefinition::order))
                .toList();
    }

    private static void requireUniqueChannels(List<AnimationChannelDefinition> channels) {
        Set<AnimationChannelId> seen = new HashSet<>();
        for (AnimationChannelDefinition channel : channels) {
            if (!seen.add(channel.id())) {
                throw new IllegalArgumentException("Duplicate animation channel: " + channel.id());
            }
        }
    }

    private static void requireUniqueStates(List<AnimationStateDefinition> states) {
        Set<AnimationStateId> seen = new HashSet<>();
        for (AnimationStateDefinition state : states) {
            if (!seen.add(state.id())) {
                throw new IllegalArgumentException("Duplicate animation state: " + state.id());
            }
        }
    }

    private static void requireInitialStates(
            List<AnimationChannelDefinition> channels,
            List<AnimationStateDefinition> states
    ) {
        for (AnimationChannelDefinition channel : channels) {
            Optional<AnimationStateDefinition> initial = states.stream()
                    .filter(state -> state.id().equals(channel.initialStateId()))
                    .findFirst();
            if (initial.isEmpty() || !initial.orElseThrow().channelId().equals(channel.id())) {
                throw new IllegalArgumentException("Channel initial state is missing: " + channel.id());
            }
        }
    }

    private static void requireTransitionStates(
            List<AnimationTransitionDefinition> transitions,
            List<AnimationStateDefinition> states
    ) {
        for (AnimationTransitionDefinition transition : transitions) {
            requireTransitionEndpoint(transition.fromStateId(), transition.channelId(), states);
            requireTransitionEndpoint(transition.toStateId(), transition.channelId(), states);
        }
    }

    private static void requireTransitionEndpoint(
            AnimationStateId stateId,
            AnimationChannelId channelId,
            List<AnimationStateDefinition> states
    ) {
        boolean valid = states.stream()
                .anyMatch(state -> state.id().equals(stateId) && state.channelId().equals(channelId));
        if (!valid) {
            throw new IllegalArgumentException("Transition endpoint is missing: " + stateId);
        }
    }

    private static void requireUnambiguousOverrideLayers(List<AnimationChannelDefinition> channels) {
        for (int left = 0; left < channels.size(); left++) {
            for (int right = left + 1; right < channels.size(); right++) {
                requireNoOverrideConflict(channels.get(left), channels.get(right));
            }
        }
    }

    private static void requireNoOverrideConflict(
            AnimationChannelDefinition left,
            AnimationChannelDefinition right
    ) {
        if (left.compositionPolicy() != ChannelCompositionPolicy.OVERRIDE
                || right.compositionPolicy() != ChannelCompositionPolicy.OVERRIDE) {
            return;
        }
        if (left.visualOrder() != right.visualOrder() || left.boneMask().isEmpty() || right.boneMask().isEmpty()) {
            return;
        }
        if (!Collections.disjoint(left.boneMask(), right.boneMask())) {
            throw new IllegalArgumentException("Overlapping override channels share visual order: " + left.id());
        }
    }
}
