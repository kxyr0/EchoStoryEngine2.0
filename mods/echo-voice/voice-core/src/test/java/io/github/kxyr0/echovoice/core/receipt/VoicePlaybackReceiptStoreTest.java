package io.github.kxyr0.echovoice.core.receipt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import org.junit.jupiter.api.Test;

class VoicePlaybackReceiptStoreTest {
    @Test
    void completedPresentationDoesNotReplayButInterruptedDoesOnce() {
        VoicePlaybackReceiptStore store = new VoicePlaybackReceiptStore(8);
        VoiceReceiptKey completed = key(1, "done");
        VoiceReceiptKey interrupted = key(2, "resume");
        store.record(completed, VoiceReceiptStatus.COMPLETED);
        store.record(interrupted, VoiceReceiptStatus.INTERRUPTED);

        assertFalse(store.shouldReplay(completed));
        assertTrue(store.shouldReplay(interrupted));
    }

    @Test
    void multiplayerSessionsRemainSeparated() {
        VoicePlaybackReceiptStore store = new VoicePlaybackReceiptStore(8);
        VoiceReceiptKey first = key("player-a", 1, "line");
        VoiceReceiptKey second = key("player-b", 1, "line");
        store.record(first, VoiceReceiptStatus.COMPLETED);

        assertFalse(store.shouldReplay(first));
        assertTrue(store.shouldReplay(second));
    }

    private VoiceReceiptKey key(int revision, String stem) {
        return key("player-a", revision, stem);
    }

    private VoiceReceiptKey key(String session, int revision, String stem) {
        return new VoiceReceiptKey(new DialogueSessionId(session), revision,
                VoiceAssetKey.of(DialogueId.parse("jdh:first_morning"), stem));
    }
}
