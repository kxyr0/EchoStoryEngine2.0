package io.github.kxyr0.echoitem.core.manifest;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.ItemKindId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Separates startup registration from live pending-restart discovery.
 */
public final class ItemRegistrationPlanner {
    public ItemRegistrationPlan startup(RegisteredItemManifest manifest, List<EchoItemId> discovered) {
        Set<EchoItemId> ids = new LinkedHashSet<>(discovered);
        ids.addAll(manifest.ids());
        List<EchoItemId> missing = manifest.ids().stream().filter(id -> !discovered.contains(id)).toList();
        return new ItemRegistrationPlan(ids.stream().sorted().toList(), List.of(), missing, List.of());
    }

    public ItemRegistrationPlan live(RegisteredItemManifest manifest, List<EchoItemId> active, List<EchoItemId> seen) {
        List<PendingItemRegistration> pending = manifest.records().stream()
                .filter(RegisteredItemRecord::pendingRegistration)
                .filter(record -> !active.contains(record.id()))
                .map(record -> pending(record.id(), record.kind()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (EchoItemId id : seen) {
            if (!active.contains(id) && pending.stream().noneMatch(candidate -> candidate.id().equals(id))
                    && manifest.find(id).isEmpty()) {
                pending.add(pending(id, ItemKindId.WEAPON));
            }
        }
        return new ItemRegistrationPlan(active, pending, List.of(), List.of());
    }

    private PendingItemRegistration pending(EchoItemId id, ItemKindId kind) {
        return new PendingItemRegistration(id, kind,
                "New item " + id + " is valid and will be registered after restart.");
    }
}
