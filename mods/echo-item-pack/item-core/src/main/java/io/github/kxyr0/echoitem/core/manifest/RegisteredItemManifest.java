package io.github.kxyr0.echoitem.core.manifest;

import io.github.kxyr0.echoitem.api.EchoItemId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Machine-owned manifest preserving registry identities across source deletion.
 */
public record RegisteredItemManifest(List<RegisteredItemRecord> records) {
    public RegisteredItemManifest {
        records = records.stream()
                .sorted(Comparator.comparing(record -> record.id().value()))
                .toList();
    }

    public static RegisteredItemManifest empty() {
        return new RegisteredItemManifest(List.of());
    }

    public Optional<RegisteredItemRecord> find(EchoItemId id) {
        Objects.requireNonNull(id, "id");
        return records.stream().filter(record -> record.id().equals(id)).findFirst();
    }

    public List<EchoItemId> ids() {
        return records.stream().map(RegisteredItemRecord::id).toList();
    }
}
