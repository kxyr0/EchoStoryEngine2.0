package io.github.kxyr0.echoanimation.forge.playback.provisioning;

import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.definition.resource.AnimationResourceSet;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeSavedData;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.SubjectRecord;

/**
 * Backward-compatible channel initializer for manual A3 subject creation.
 */
public final class AnimationSubjectChannelInitializer {
    public boolean inferChannels(
            AnimationRuntimeSavedData data,
            AnimationResourceSet resources,
            SubjectRecord subject
    ) {
        boolean changed = false;
        for (AnimationDefinition definition : resources.animations()) {
            AnimationStateId state = initialState(definition);
            for (var channel : definition.controlledChannels()) {
                if (!subject.channelStates.containsKey(channel)) {
                    subject.channelStates.put(channel, state);
                    changed = true;
                }
            }
        }
        if (changed) {
            subject.revision++;
            data.setDirtyRevision();
        }
        return changed;
    }

    private AnimationStateId initialState(AnimationDefinition definition) {
        return definition.allowedSourceStates().stream()
                .filter(state -> state.value().endsWith("/idle") || state.value().equals("idle"))
                .findFirst()
                .orElse(definition.allowedSourceStates().stream().sorted().findFirst()
                        .orElse(definition.targetState()));
    }
}
