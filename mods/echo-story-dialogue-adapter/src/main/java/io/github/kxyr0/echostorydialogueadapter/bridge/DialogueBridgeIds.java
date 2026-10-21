package io.github.kxyr0.echostorydialogueadapter.bridge;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueOperationStatus;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionStatus;
import java.util.Optional;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;

/**
 * Stable public identifiers owned by the adapter.
 */
final class DialogueBridgeIds {
    static final String IMPLEMENTATION_VERSION = "0.3.0-SNAPSHOT";
    static final NamespacedId EXTENSION_ID = new NamespacedId("echo_story_dialogue_adapter", "bridge");
    static final NamespacedId ACTION_TYPE = new NamespacedId("echo_dialogue", "play");

    private DialogueBridgeIds() {
    }

    static DialogueSessionId dialogueSessionId(ActionExecutionId actionExecutionId) {
        return new DialogueSessionId("story-action/" + actionExecutionId.asString());
    }
    static boolean sameActionSession(
            DialogueOperationStatus status,
            Optional<DialogueSessionView> session,
            io.github.kxyr0.echodialogue.api.identity.DialogueSessionId expected
    ) {
        return status != DialogueOperationStatus.CONFLICT
                && session.map(DialogueSessionView::sessionId)
                .filter(id -> id.equals(expected))
                .isPresent();
    }

    static boolean replaceableStoryConflict(
            DialogueOperationStatus status,
            Optional<DialogueSessionView> session,
            DialogueActionPayload decoded
    ) {
        return status == DialogueOperationStatus.CONFLICT
                && session.filter(DialogueBridgeIds::activeOrWaiting)
                .filter(value -> value.sessionId().toString().startsWith("story-action/"))
                .filter(value -> !value.sessionId().equals(decoded.sessionId()))
                .filter(value -> value.audienceId().equals(decoded.audienceId()))
                .filter(value -> value.dialogueId().equals(decoded.dialogueId()))
                .isPresent();
    }

    static boolean activeOrWaiting(DialogueSessionView session) {
        return session.status() == DialogueSessionStatus.ACTIVE
                || session.status() == DialogueSessionStatus.WAITING;
    }

}
