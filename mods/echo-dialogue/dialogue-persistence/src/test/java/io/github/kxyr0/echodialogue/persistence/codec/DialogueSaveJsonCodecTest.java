package io.github.kxyr0.echodialogue.persistence.codec;

import static io.github.kxyr0.echodialogue.persistence.PersistenceTestFixtures.activeCheckpoint;
import static io.github.kxyr0.echodialogue.persistence.PersistenceTestFixtures.completedCheckpoint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.outcome.DialogueOutcomeId;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echodialogue.core.fingerprint.CompiledDialogueFingerprint;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryRevision;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DialogueSaveJsonCodecTest {
    private final DialogueSaveJsonCodec codec = new DialogueSaveJsonCodec();

    @Test
    void activeLineCheckpointRoundTripsAsSchemaV2() {
        DialogueCheckpoint checkpoint = activeCheckpoint(1000);

        DialogueCheckpoint decoded = codec.decode(codec.encode(checkpoint));

        assertEquals(checkpoint, decoded);
        assertEquals(DialoguePresentationKind.LINE, decoded.presentationKind());
    }

    @Test
    void choiceCheckpointRoundTripsAsSchemaV2() {
        DialogueCheckpoint checkpoint = new DialogueCheckpoint(
                new DialogueSessionId("choice-session"),
                DialogueId.parse("sample:question"),
                new CompiledDialogueFingerprint(hex()),
                new AudienceId("audience-1"),
                DialogueSessionPhase.ACTIVE,
                4,
                new DialogueNodeId("question"),
                DialoguePresentationKind.CHOICE,
                0,
                1000,
                1000,
                Optional.empty(),
                Optional.empty(),
                Map.of(),
                new DialogueRepositoryRevision(2));

        DialogueCheckpoint decoded = codec.decode(codec.encode(checkpoint));

        assertEquals(DialoguePresentationKind.CHOICE, decoded.presentationKind());
        assertEquals(checkpoint, decoded);
    }

    @Test
    void completedCheckpointRoundTripsWithDefaultOutcome() {
        DialogueCheckpoint decoded = codec.decode(codec.encode(completedCheckpoint(1000)));

        assertEquals(DialogueOutcomeId.DEFAULT, decoded.terminalOutcomeId().orElseThrow());
    }

    @Test
    void waitingCheckpointRoundTripsExternalWaitThroughMetadata() {
        DialogueExternalWait wait = new DialogueExternalWait(
                "echo_trigger", "trigger-instance-1", "PlayerEnter(Exit)",
                new DialogueNodeId("greeting"), 3);
        DialogueCheckpoint checkpoint = new DialogueCheckpoint(
                new DialogueSessionId("wait-session"),
                DialogueId.parse("sample:greeting"),
                new CompiledDialogueFingerprint(hex()),
                new AudienceId("audience-1"),
                DialogueSessionPhase.WAITING,
                4,
                new DialogueNodeId("greeting"),
                DialoguePresentationKind.LINE,
                0,
                1000,
                1000,
                Optional.empty(),
                Optional.empty(),
                Map.of(
                        "externalWait.type", wait.waitType(),
                        "externalWait.key", wait.waitKey(),
                        "externalWait.payload", wait.payload(),
                        "externalWait.node", wait.continuationNodeId().toString(),
                        "externalWait.commandPosition", Integer.toString(wait.commandPosition())),
                new DialogueRepositoryRevision(2));

        DialogueCheckpoint decoded = codec.decode(codec.encode(checkpoint));

        assertEquals(DialogueSessionPhase.WAITING, decoded.phase());
        assertEquals(wait, decoded.externalWait().orElseThrow());
    }

    @Test
    void legacySchemaV1CompletedMigratesToDefaultOutcome() {
        byte[] json = """
                {
                  "saveSchemaVersion":1,
                  "sessionId":"legacy-session",
                  "dialogueId":"sample:greeting",
                  "compiledFingerprint":"0000000000000000000000000000000000000000000000000000000000000000",
                  "audienceId":"audience-1",
                  "phase":"COMPLETED",
                  "revision":3,
                  "currentNodeId":"greeting",
                  "lineOrdinal":0,
                  "createdAtEpochMillis":1,
                  "updatedAtEpochMillis":2,
                  "terminalResult":"completed",
                  "cancellationReason":null,
                  "metadata":{},
                  "repositoryRevision":1
                }
                """.getBytes(StandardCharsets.UTF_8);

        DialogueCheckpoint decoded = codec.decode(json);

        assertEquals(DialogueOutcomeId.DEFAULT, decoded.terminalOutcomeId().orElseThrow());
        assertEquals(DialoguePresentationKind.LINE, decoded.presentationKind());
    }

    private static String hex() {
        return "0000000000000000000000000000000000000000000000000000000000000000";
    }

    @Test
    void unsupportedSchemaVersionIsRejected() {
        byte[] json = """
                {"saveSchemaVersion":99}
                """.getBytes(StandardCharsets.UTF_8);

        assertThrows(DialogueSaveJsonCodec.SaveCodecException.class, () -> codec.decode(json));
    }
}
