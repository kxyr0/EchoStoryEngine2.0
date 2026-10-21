package io.github.kxyr0.echobackgroundnpc.core.conversation;

import io.github.kxyr0.echobackgroundnpc.api.conversation.AmbientDialogue;
import io.github.kxyr0.echobackgroundnpc.api.conversation.AmbientLine;
import io.github.kxyr0.echobackgroundnpc.api.conversation.ConversationOutcome;
import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import java.util.Objects;
import java.util.Optional;

/** Revision-pinned linear noninteractive session. */
public final class AmbientConversationSession {
    private final String id;
    private final BackgroundNpcId first;
    private final BackgroundNpcId second;
    private final AmbientDialogue dialogue;
    private int lineIndex;
    private long nextLineAt;
    private ConversationOutcome outcome;

    public AmbientConversationSession(String id, BackgroundNpcId first, BackgroundNpcId second,
            AmbientDialogue dialogue, long nowMillis, AmbientReadingTime timing) {
        this.id = Objects.requireNonNull(id, "id");
        this.first = Objects.requireNonNull(first, "first");
        this.second = Objects.requireNonNull(second, "second");
        this.dialogue = Objects.requireNonNull(dialogue, "dialogue");
        nextLineAt = nowMillis + timing.forText(current().text());
    }

    public Optional<AmbientLine> advance(long nowMillis, AmbientReadingTime timing) {
        if (outcome != null || nowMillis < nextLineAt) return Optional.empty();
        lineIndex++;
        if (lineIndex >= dialogue.lines().size()) { outcome = ConversationOutcome.COMPLETED; return Optional.empty(); }
        nextLineAt = nowMillis + timing.forText(current().text());
        return Optional.of(current());
    }

    public void preempt() { if (outcome == null) outcome = ConversationOutcome.PREEMPTED; }
    public AmbientLine current() { return dialogue.lines().get(lineIndex); }
    public boolean active() { return outcome == null; }
    public Optional<ConversationOutcome> outcome() { return Optional.ofNullable(outcome); }
    public String id() { return id; }
    public BackgroundNpcId first() { return first; }
    public BackgroundNpcId second() { return second; }
    public AmbientDialogue dialogue() { return dialogue; }
    public int lineIndex() { return lineIndex; }
    public long remainingMillis(long nowMillis) { return Math.max(1L, nextLineAt - nowMillis); }
}
