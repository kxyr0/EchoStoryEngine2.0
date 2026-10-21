package io.github.kxyr0.echonpcbackpack.core.command;

import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackCommandId;
import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackCommandResult;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class BackpackCommandLedger {
    private final Map<NpcBackpackCommandId, Entry> entries = new LinkedHashMap<>();

    public Claim claim(NpcBackpackCommandId id, String fingerprint) {
        Entry existing = entries.get(id);
        if (existing == null) {
            entries.put(id, new Entry(fingerprint, Optional.empty()));
            return Claim.NEW;
        }
        return existing.fingerprint.equals(fingerprint) ? Claim.DUPLICATE : Claim.CONFLICT;
    }

    public void complete(NpcBackpackCommandId id, NpcBackpackCommandResult result) {
        Entry existing = entries.get(id);
        if (existing == null) throw new IllegalStateException("Unknown command id: " + id);
        entries.put(id, new Entry(existing.fingerprint, Optional.of(result)));
    }

    public Optional<NpcBackpackCommandResult> result(NpcBackpackCommandId id) {
        return Optional.ofNullable(entries.get(id)).flatMap(Entry::result);
    }

    public enum Claim { NEW, DUPLICATE, CONFLICT }

    private record Entry(String fingerprint, Optional<NpcBackpackCommandResult> result) {
    }
}
