package io.github.kxyr0.echodialoguevoice.lifecycle;

import io.github.kxyr0.echodialogue.forge.api.client.DialogueLinePresentationSnapshot;
import io.github.kxyr0.echodialogue.forge.api.client.EchoDialogueLineEnterCommittedEvent;
import io.github.kxyr0.echodialogue.forge.api.client.EchoDialogueLineExitCommittedEvent;
import io.github.kxyr0.echodialogue.forge.api.client.EchoDialogueTypewriterRevealEvent;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import io.github.kxyr0.echovoice.api.identity.VoicePlaybackId;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackOwner;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackReceipt;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackRequest;
import io.github.kxyr0.echovoice.api.playback.VoiceStopReason;
import io.github.kxyr0.echovoice.forge.api.EchoVoiceServiceLookupEvent;
import io.github.kxyr0.echovoice.forge.api.VoicePlaybackLifecycleListener;
import io.github.kxyr0.echovoice.forge.api.VoiceQueryService;
import io.github.kxyr0.echovoice.forge.api.VoiceService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraftforge.common.MinecraftForge;

/** Converts only committed Dialogue transitions into foreground voice commands. */
public final class DialogueVoiceLifecycleBridge implements VoicePlaybackLifecycleListener {
    private final DialogueVoiceReceipts receipts = new DialogueVoiceReceipts(512);
    private final Map<VoicePlaybackId, DialogueVoiceReceipts.Key> attempts = new HashMap<>();
    private VoiceService service;
    private VoiceQueryService query;
    private DialogueLinePresentationSnapshot current;
    private DialogueVoiceMetadata metadata;
    private boolean playbackRequested;
    private long generation;
    private long typewriterReveals;
    private long staleEventRejects;
    private boolean pendingAdvance;

    public DialogueVoiceLifecycleBridge() {
    }

    DialogueVoiceLifecycleBridge(VoiceService service, VoiceQueryService query) {
        this.service = service;
        this.query = query;
        service.addLifecycleListener(this);
    }

    public void enter(EchoDialogueLineEnterCommittedEvent event) {
        DialogueLinePresentationSnapshot next = event.presentation();
        if (samePresentation(current, next)) {
            staleEventRejects++;
            return;
        }
        current = next;
        metadata = DialogueVoiceMetadata.from(next.cues()).orElse(null);
        playbackRequested = false;
        pendingAdvance = false;
        if (metadata == null || !resolveService()) {
            return;
        }
        VoiceAssetKey assetKey = new VoiceAssetKey(next.dialogueId(), metadata.stem());
        DialogueVoiceReceipts.Key receiptKey = new DialogueVoiceReceipts.Key(next.sessionId(), next.revision(),
                assetKey);
        if (receipts.find(receiptKey).filter(status -> status == DialogueVoiceReceipts.Status.COMPLETED).isPresent()) {
            return;
        }
        VoicePlaybackRequest request = new VoicePlaybackRequest(next.sessionId(), next.revision(), ++generation,
                assetKey, metadata.volume(), VoicePlaybackOwner.DIALOGUE);
        VoicePlaybackReceipt receipt = service.play(request);
        playbackRequested = true;
        attempts.put(receipt.playbackId(), receiptKey);
    }

    public void exit(EchoDialogueLineExitCommittedEvent event) {
        if (!samePresentation(current, event.presentation())) {
            staleEventRejects++;
            return;
        }
        pendingAdvance = event.reason() == EchoDialogueLineExitCommittedEvent.Reason.LINE_ADVANCED;
        if (playbackRequested && service != null) {
            service.stop(stopReason(event.reason()));
        }
        current = null;
        metadata = null;
        playbackRequested = false;
    }

    public void reveal(EchoDialogueTypewriterRevealEvent event) {
        if (samePresentation(current, event.presentation())) {
            typewriterReveals++;
        } else {
            staleEventRejects++;
        }
    }

    public java.util.List<String> statusLines() {
        String dialogue = current == null ? "NONE" : current.dialogueId().toString();
        String presentation = current == null ? "NONE" : current.sessionId() + ":" + current.revision();
        String speaker = current == null ? "NONE"
                : current.speakerId().map(Object::toString).orElse("NONE");
        String stem = metadata == null ? "NONE" : metadata.stem().value();
        String playback = query == null ? "UNAVAILABLE" : query.playback().snapshot().status().name();
        return java.util.List.of("dialogue=" + dialogue + " node=UNAVAILABLE_PUBLIC_API presentation=" + presentation,
                "speaker=" + speaker + " has_voice=" + (metadata != null) + " stem=" + stem,
                "committed_revision=" + (current == null ? "NONE" : current.revision())
                        + " typewriter_reveals=" + typewriterReveals + " pending_advance=" + pendingAdvance,
                "active_playback=" + playback + " stale_event_rejects=" + staleEventRejects);
    }

    @Override
    public void started(VoicePlaybackReceipt receipt) {
        record(receipt.playbackId(), DialogueVoiceReceipts.Status.PLAYING, false);
    }

    @Override
    public void completed(VoicePlaybackReceipt receipt) {
        record(receipt.playbackId(), DialogueVoiceReceipts.Status.COMPLETED, true);
    }

    @Override
    public void interrupted(VoicePlaybackReceipt receipt) {
        record(receipt.playbackId(), DialogueVoiceReceipts.Status.INTERRUPTED, true);
    }

    @Override
    public void failed(VoicePlaybackReceipt receipt) {
        attempts.remove(receipt.playbackId());
    }

    private boolean resolveService() {
        if (service != null) {
            return true;
        }
        EchoVoiceServiceLookupEvent lookup = new EchoVoiceServiceLookupEvent();
        MinecraftForge.EVENT_BUS.post(lookup);
        if (lookup.providers().size() != 1) {
            return false;
        }
        EchoVoiceServiceLookupEvent.Entry provider = lookup.providers().get(0);
        service = provider.service();
        query = provider.query();
        service.addLifecycleListener(this);
        return true;
    }

    private void record(VoicePlaybackId id, DialogueVoiceReceipts.Status status, boolean terminal) {
        Optional.ofNullable(attempts.get(id)).ifPresent(key -> receipts.record(key, status));
        if (terminal) {
            attempts.remove(id);
        }
    }

    private boolean samePresentation(DialogueLinePresentationSnapshot left,
            DialogueLinePresentationSnapshot right) {
        return left != null && left.sessionId().equals(right.sessionId()) && left.revision() == right.revision();
    }

    private VoiceStopReason stopReason(EchoDialogueLineExitCommittedEvent.Reason reason) {
        return switch (reason) {
            case LINE_ADVANCED -> VoiceStopReason.LINE_ADVANCED;
            case DIALOGUE_COMPLETED -> VoiceStopReason.DIALOGUE_COMPLETED;
            case DIALOGUE_CANCELLED -> VoiceStopReason.DIALOGUE_CANCELLED;
            case DIALOGUE_REPLACED -> VoiceStopReason.DIALOGUE_REPLACED;
            case PLAYER_DISCONNECTED -> VoiceStopReason.PLAYER_DISCONNECTED;
            case CLIENT_RESET -> VoiceStopReason.WORLD_UNLOADED;
        };
    }
}
