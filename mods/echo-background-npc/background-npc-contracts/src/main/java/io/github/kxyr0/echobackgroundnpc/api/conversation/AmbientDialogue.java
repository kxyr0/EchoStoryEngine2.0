package io.github.kxyr0.echobackgroundnpc.api.conversation;

import io.github.kxyr0.echobackgroundnpc.api.identity.resource.AmbientDialogueId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record AmbientDialogue(AmbientDialogueId id, List<AmbientLine> lines,
        Optional<AmbientQuestOffer> completionQuest, long revision) {
    public AmbientDialogue {
        id = Objects.requireNonNull(id, "id");
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        completionQuest = Objects.requireNonNull(completionQuest, "completionQuest");
        if (lines.isEmpty() || revision < 0L) throw new IllegalArgumentException("Invalid ambient dialogue.");
    }

    public AmbientDialogue(AmbientDialogueId id, List<AmbientLine> lines, long revision) {
        this(id, lines, Optional.empty(), revision);
    }
}
