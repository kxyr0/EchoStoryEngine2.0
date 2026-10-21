package io.github.kxyr0.echostorydialogueadapter.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueOperationStatus;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionStatus;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DialogueActionPayloadTest {
    @Test
    void decodeAcceptsOnlyDialogueIdAndAudienceId() {
        DialogueActionPayload decoded = DialogueActionPayload.decode(Map.of(
                "dialogueId", "sample:greeting",
                "audienceId", "player-1"), ActionExecutionId.of("sample:action_1"));

        assertEquals("sample:greeting", decoded.dialogueId().toString());
        assertEquals("player-1", decoded.audienceId().toString());
        assertEquals("story-action/sample:action_1", decoded.sessionId().toString());
    }

    @Test
    void decodeInfersAudienceFromStoryExecutionPlayer() {
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000123");
        DialogueActionPayload decoded = DialogueActionPayload.decode(
                Map.of("dialogueId", "sample:greeting"),
                ExecutionId.of("sample:execution/player/" + playerId + "/scene/1"),
                ActionExecutionId.of("sample:action_1"), null);

        assertEquals(playerId.toString(), decoded.audienceId().toString());
    }

    @Test
    void decodeRejectsExtraArguments() {
        assertThrows(IllegalArgumentException.class, () -> DialogueActionPayload.decode(Map.of(
                "dialogueId", "sample:greeting",
                "audienceId", "player-1",
                "choice", "later"), ActionExecutionId.of("sample:action_1")));
    }

    @Test
    void storyConflictCanReplaceStaleStorySessionForSameAudienceAndDialogue() {
        DialogueActionPayload decoded = DialogueActionPayload.decode(Map.of(
                "dialogueId", "sample:greeting",
                "audienceId", "player-1"), ActionExecutionId.of("sample:current_action"));
        DialogueSessionView stale = view("story-action/sample:old_action", "sample:greeting", "player-1",
                DialogueSessionStatus.WAITING);

        boolean replaceable = DialoguePlayActionHandler.replaceableStoryConflict(
                DialogueOperationStatus.CONFLICT, Optional.of(stale), decoded);

        assertEquals(true, replaceable);
    }

    @Test
    void storyConflictDoesNotReplaceManualSession() {
        DialogueActionPayload decoded = DialogueActionPayload.decode(Map.of(
                "dialogueId", "sample:greeting",
                "audienceId", "player-1"), ActionExecutionId.of("sample:current_action"));
        DialogueSessionView manual = view("manual-session", "sample:greeting", "player-1",
                DialogueSessionStatus.ACTIVE);

        boolean replaceable = DialoguePlayActionHandler.replaceableStoryConflict(
                DialogueOperationStatus.CONFLICT, Optional.of(manual), decoded);

        assertEquals(false, replaceable);
    }

    private DialogueSessionView view(String sessionId, String dialogueId, String audienceId,
            DialogueSessionStatus status) {
        return new DialogueSessionView(new DialogueSessionId(sessionId), DialogueId.parse(dialogueId),
                new AudienceId(audienceId), status, 1, 1, Optional.empty(), Optional.empty(), Optional.empty());
    }
}
