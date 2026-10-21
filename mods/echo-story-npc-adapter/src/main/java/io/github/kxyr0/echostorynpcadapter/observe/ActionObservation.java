package io.github.kxyr0.echostorynpcadapter.observe;

import io.github.kxyr0.echonpc.api.block.BlockId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import java.util.Objects;
import java.util.Optional;

public record ActionObservation(
        Kind kind,
        StoryId storyId,
        ExecutionId executionId,
        ActionExecutionId actionExecutionId,
        NamespacedId actionType,
        ActorMutationId mutationId,
        ActorId actorId,
        Optional<NpcDefinitionId> definitionId,
        Optional<NpcWorldId> worldId,
        Optional<NpcPosition> position,
        Optional<NpcRotation> rotation,
        Optional<BlockPosition> bedPosition,
        Optional<BlockId> expectedBlock,
        String successResult,
        String payloadFingerprint,
        ActionCompletionSink completionSink) {
    public ActionObservation {
        kind = Objects.requireNonNull(kind, "kind");
        storyId = Objects.requireNonNull(storyId, "storyId");
        executionId = Objects.requireNonNull(executionId, "executionId");
        actionExecutionId = Objects.requireNonNull(actionExecutionId, "actionExecutionId");
        actionType = Objects.requireNonNull(actionType, "actionType");
        mutationId = Objects.requireNonNull(mutationId, "mutationId");
        actorId = Objects.requireNonNull(actorId, "actorId");
        definitionId = Objects.requireNonNull(definitionId, "definitionId");
        worldId = Objects.requireNonNull(worldId, "worldId");
        position = Objects.requireNonNull(position, "position");
        rotation = Objects.requireNonNull(rotation, "rotation");
        bedPosition = Objects.requireNonNull(bedPosition, "bedPosition");
        expectedBlock = Objects.requireNonNull(expectedBlock, "expectedBlock");
        successResult = Objects.requireNonNull(successResult, "successResult");
        payloadFingerprint = Objects.requireNonNull(payloadFingerprint, "payloadFingerprint");
        completionSink = Objects.requireNonNull(completionSink, "completionSink");
    }

    public String key() {
        return actionExecutionId.asString();
    }

    public enum Kind {
        SPAWN,
        TELEPORT,
        DESPAWN,
        SLEEP,
        WAKE
    }
}
