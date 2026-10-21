package io.github.kxyr0.echoaudio.forge.dialogue;

import io.github.kxyr0.echoaudio.api.AudioCue;
import io.github.kxyr0.echodialogue.api.command.DialogueCommandPayload;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.observation.DialogueObservationHandle;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalResumeRequest;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** One dialogue owns its loops and outstanding waits; no voice lanes or global stops. */
public final class AudioDialogueSession implements AutoCloseable {
    private final DialogueService service;
    private final DialogueSessionId id;
    private final Runnable stop;
    private final Set<String> dispatched = new LinkedHashSet<>();
    private DialogueObservationHandle observation;
    private boolean closed;

    public AudioDialogueSession(DialogueService service, DialogueSessionId id, Runnable stop) {
        this.service = service; this.id = id; this.stop = stop;
    }
    public void observe() { observation = service.observeTerminal(id, event -> close()); }
    public void dispatch(DialogueExternalWait wait, BiConsumer<AudioCue, Consumer<String>> playback) {
        var current = service.query(id).session();
        if (closed || current.isEmpty() || !current.orElseThrow().externalWait().filter(wait::equals).isPresent()) return;
        int revision = current.orElseThrow().revision();
        String receipt = wait.waitKey() + "@" + revision;
        if (!dispatched.add(receipt)) return;
        if (dispatched.size() > 1024) dispatched.remove(dispatched.iterator().next());
        try {
            AudioCue cue = AudioCue.decode(DialogueCommandPayload.decode(wait.payload()));
            playback.accept(cue, detail -> {
                if (cue.waitForEnd()) finish(wait, revision, detail);
                else if (!detail.isEmpty()) com.mojang.logging.LogUtils.getLogger().warn("{}", detail);
            });
            if (!cue.waitForEnd()) finish(wait, revision, "");
        } catch (RuntimeException failure) {
            com.mojang.logging.LogUtils.getLogger().warn("AUDIO_DIALOGUE_FAILED: {}", failure.getMessage());
            finish(wait, revision, "AUDIO_PLAYBACK_START_FAILED");
        }
    }
    private void finish(DialogueExternalWait wait, int revision, String detail) {
        if (closed) return;
        if (!detail.isEmpty()) com.mojang.logging.LogUtils.getLogger().warn("{}: {}", wait.waitKey(), detail);
        service.query(id).session().filter(session -> session.revision() == revision && session.externalWait().filter(wait::equals).isPresent())
                .ifPresent(session -> {
                    if (detail.isEmpty()) service.resumeExternal(
                            new DialogueExternalResumeRequest(id, session.revision(), wait.waitKey()));
                    else service.cancel(id);
                });
    }
    @Override public void close() {
        if (closed) return;
        closed = true;
        stop.run();
        if (observation != null) observation.close();
        dispatched.clear();
    }
}
