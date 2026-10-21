package io.github.kxyr0.echonpcbackpack.forge.state;

import java.util.LinkedHashMap;
import java.util.Map;

public record BackpackJournalSnapshot(Map<String, BackpackActorRecord> actors,
        Map<String, String> terminalCommands, long repositoryRevision) {
    public BackpackJournalSnapshot {
        actors = new LinkedHashMap<>(actors);
        terminalCommands = new LinkedHashMap<>(terminalCommands);
        repositoryRevision = Math.max(1L, repositoryRevision);
    }

    public static BackpackJournalSnapshot empty() {
        return new BackpackJournalSnapshot(Map.of(), Map.of(), 1L);
    }
}
