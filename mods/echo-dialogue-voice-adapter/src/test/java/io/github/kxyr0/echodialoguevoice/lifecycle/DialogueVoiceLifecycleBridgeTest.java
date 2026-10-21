package io.github.kxyr0.echodialoguevoice.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.forge.api.client.DialogueLineCue;
import io.github.kxyr0.echodialogue.forge.api.client.DialogueLinePresentationSnapshot;
import io.github.kxyr0.echodialogue.forge.api.client.EchoDialogueLineEnterCommittedEvent;
import io.github.kxyr0.echodialogue.forge.api.client.EchoDialogueLineExitCommittedEvent;
import io.github.kxyr0.echodialogue.forge.api.client.EchoDialogueTypewriterRevealEvent;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackOutcome;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackReceipt;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackRequest;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackSnapshot;
import io.github.kxyr0.echovoice.api.playback.VoiceStopReason;
import io.github.kxyr0.echovoice.forge.api.VoiceAssetCatalog;
import io.github.kxyr0.echovoice.forge.api.VoiceDiagnosticsView;
import io.github.kxyr0.echovoice.forge.api.VoicePlaybackLifecycleListener;
import io.github.kxyr0.echovoice.forge.api.VoicePlaybackView;
import io.github.kxyr0.echovoice.forge.api.VoiceQueryService;
import io.github.kxyr0.echovoice.forge.api.VoiceService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DialogueVoiceLifecycleBridgeTest {
    @Test
    void typewriterRevealDoesNotStopButCommittedAdvanceDoes() {
        FakeVoiceService voice = new FakeVoiceService();
        DialogueVoiceLifecycleBridge bridge = new DialogueVoiceLifecycleBridge(voice, query());
        DialogueLinePresentationSnapshot line = snapshot(1, "john_001");
        bridge.enter(new EchoDialogueLineEnterCommittedEvent(line));

        bridge.reveal(new EchoDialogueTypewriterRevealEvent(line));
        assertEquals(0, voice.stops.size());
        bridge.exit(new EchoDialogueLineExitCommittedEvent(line,
                EchoDialogueLineExitCommittedEvent.Reason.LINE_ADVANCED));

        assertEquals(List.of(VoiceStopReason.LINE_ADVANCED), voice.stops);
    }

    @Test
    void duplicateCommittedEnterDoesNotRestart() {
        FakeVoiceService voice = new FakeVoiceService();
        DialogueVoiceLifecycleBridge bridge = new DialogueVoiceLifecycleBridge(voice, query());
        DialogueLinePresentationSnapshot line = snapshot(1, "john_001");

        bridge.enter(new EchoDialogueLineEnterCommittedEvent(line));
        bridge.enter(new EchoDialogueLineEnterCommittedEvent(line));

        assertEquals(1, voice.requests.size());
    }

    @Test
    void silentLineExitsDoNotStopVoiceLaneOrInvalidateNextVoice() {
        FakeVoiceService voice = new FakeVoiceService();
        DialogueVoiceLifecycleBridge bridge = new DialogueVoiceLifecycleBridge(voice, query());
        DialogueLinePresentationSnapshot first = snapshot(1, "unknown_001");
        DialogueLinePresentationSnapshot silentOne = silentSnapshot(2);
        DialogueLinePresentationSnapshot silentTwo = silentSnapshot(3);
        DialogueLinePresentationSnapshot second = snapshot(4, "unknown_002");

        enterAndAdvance(bridge, first);
        enterAndAdvance(bridge, silentOne);
        enterAndAdvance(bridge, silentTwo);
        bridge.enter(new EchoDialogueLineEnterCommittedEvent(second));

        assertEquals(List.of(VoiceStopReason.LINE_ADVANCED), voice.stops);
        assertEquals(List.of(1L, 2L), voice.requests.stream()
                .map(VoicePlaybackRequest::presentationGeneration)
                .toList());
    }

    @Test
    void completedReceiptPreventsReconnectReplay() {
        FakeVoiceService voice = new FakeVoiceService();
        DialogueVoiceLifecycleBridge bridge = new DialogueVoiceLifecycleBridge(voice, query());
        DialogueLinePresentationSnapshot line = snapshot(3, "john_003");
        bridge.enter(new EchoDialogueLineEnterCommittedEvent(line));
        voice.completeLast();
        bridge.exit(new EchoDialogueLineExitCommittedEvent(line,
                EchoDialogueLineExitCommittedEvent.Reason.PLAYER_DISCONNECTED));

        bridge.enter(new EchoDialogueLineEnterCommittedEvent(line));

        assertEquals(1, voice.requests.size());
    }

    private DialogueLinePresentationSnapshot snapshot(int revision, String stem) {
        return new DialogueLinePresentationSnapshot(new DialogueSessionId("player-a"),
                DialogueId.parse("jdh:first_morning"), revision, Optional.of(SpeakerId.parse("jdh:john")),
                List.of(new DialogueLineCue("voice.stem", stem), new DialogueLineCue("voice.volume", "0.8")));
    }

    private DialogueLinePresentationSnapshot silentSnapshot(int revision) {
        return new DialogueLinePresentationSnapshot(new DialogueSessionId("player-a"),
                DialogueId.parse("jdh:first_morning"), revision, Optional.of(SpeakerId.parse("jdh:john")),
                List.of());
    }

    private void enterAndAdvance(DialogueVoiceLifecycleBridge bridge, DialogueLinePresentationSnapshot line) {
        bridge.enter(new EchoDialogueLineEnterCommittedEvent(line));
        bridge.exit(new EchoDialogueLineExitCommittedEvent(line,
                EchoDialogueLineExitCommittedEvent.Reason.LINE_ADVANCED));
    }

    private VoiceQueryService query() {
        VoiceAssetCatalog assets = new VoiceAssetCatalog() {
            @Override public Optional<io.github.kxyr0.echovoice.api.asset.VoiceAssetSnapshot> find(VoiceAssetKey key) {
                return Optional.empty();
            }
            @Override public List<io.github.kxyr0.echovoice.api.asset.VoiceAssetSnapshot> assets() {
                return List.of();
            }
        };
        VoicePlaybackView playback = () -> VoicePlaybackSnapshot.idle(0, 0);
        VoiceDiagnosticsView diagnostics = List::of;
        return new VoiceQueryService() {
            @Override public VoiceAssetCatalog assets() { return assets; }
            @Override public VoicePlaybackView playback() { return playback; }
            @Override public VoiceDiagnosticsView diagnostics() { return diagnostics; }
        };
    }

    private static final class FakeVoiceService implements VoiceService {
        private final List<VoicePlaybackRequest> requests = new ArrayList<>();
        private final List<VoiceStopReason> stops = new ArrayList<>();
        private VoicePlaybackLifecycleListener listener;
        private VoicePlaybackReceipt last;

        @Override public VoicePlaybackReceipt play(VoicePlaybackRequest request) {
            requests.add(request);
            last = new VoicePlaybackReceipt(io.github.kxyr0.echovoice.api.identity.VoicePlaybackId.next(),
                    VoicePlaybackOutcome.QUEUED, Optional.empty(), 0, "queued");
            return last;
        }
        @Override public VoicePlaybackReceipt stop(VoiceStopReason reason) {
            stops.add(reason);
            return new VoicePlaybackReceipt(io.github.kxyr0.echovoice.api.identity.VoicePlaybackId.next(),
                    VoicePlaybackOutcome.INTERRUPTED, Optional.of(reason), 0, "stopped");
        }
        @Override public void preload(VoiceAssetKey assetKey) { }
        @Override public void addLifecycleListener(VoicePlaybackLifecycleListener value) { listener = value; }
        @Override public void removeLifecycleListener(VoicePlaybackLifecycleListener value) { listener = null; }
        void completeLast() {
            listener.completed(new VoicePlaybackReceipt(last.playbackId(), VoicePlaybackOutcome.COMPLETED,
                    Optional.empty(), 100, "complete"));
        }
    }
}
