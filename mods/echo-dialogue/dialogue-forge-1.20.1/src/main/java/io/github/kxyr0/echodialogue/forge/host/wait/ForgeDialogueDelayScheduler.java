package io.github.kxyr0.echodialogue.forge.host.wait;

import io.github.kxyr0.echodialogue.api.command.DialogueCommandPayload;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalResumeRequest;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Server-tick scheduler for the built-in EchoScript {@code Delay(ticks)} command.
 */
public final class ForgeDialogueDelayScheduler {
    public static final String WAIT_TYPE = "echo_dialogue:delay";

    private final Map<String, PendingDelay> pending = new HashMap<>();
    private long currentTick;

    /**
     * Starts a delay after the WAITING checkpoint has been durably installed.
     * Repeated dispatch of the same wait is idempotent and does not restart its timer.
     */
    public boolean schedule(DialogueSessionView session, DialogueExternalWait wait) {
        if (!WAIT_TYPE.equals(wait.waitType())) {
            return false;
        }
        try {
            long ticks = Long.parseLong(DialogueCommandPayload.decode(wait.payload()).get("ticks"));
            if (ticks <= 0L) {
                return false;
            }
            PendingDelay value = new PendingDelay(session.sessionId(), session.revision(), wait.waitKey(),
                    dueTick(ticks));
            pending.putIfAbsent(wait.waitKey(), value);
            return true;
        } catch (IllegalArgumentException | NullPointerException exception) {
            return false;
        }
    }

    /**
     * Advances the scheduler by one Minecraft server tick and resumes due dialogue waits.
     */
    public void tick(Consumer<DialogueExternalResumeRequest> resume) {
        currentTick++;
        for (PendingDelay wait : pending.values().stream()
                .filter(value -> value.dueTick() <= currentTick)
                .sorted(java.util.Comparator.comparingLong(PendingDelay::dueTick)
                        .thenComparing(PendingDelay::waitKey))
                .toList()) {
            if (pending.remove(wait.waitKey(), wait)) {
                resume.accept(new DialogueExternalResumeRequest(wait.sessionId(), wait.expectedRevision(),
                        wait.waitKey()));
            }
        }
    }

    public void clear() {
        pending.clear();
        currentTick = 0L;
    }

    int pendingCount() {
        return pending.size();
    }

    private long dueTick(long ticks) {
        return ticks > Long.MAX_VALUE - currentTick ? Long.MAX_VALUE : currentTick + ticks;
    }

    private record PendingDelay(
            DialogueSessionId sessionId,
            int expectedRevision,
            String waitKey,
            long dueTick) {
    }
}
