package io.github.kxyr0.echocontentdoctor.provider;

import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echocontentdoctor.source.SourceSpans;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Explicit snapshot of real registry keys, including vanilla and every installed item provider. */
public record ItemSnapshotProvider(long revision, int files, String sourceId,
        Set<String> itemIds) implements ContentSnapshotProvider {
    public ItemSnapshotProvider {
        itemIds = Set.copyOf(itemIds);
        itemIds.forEach(ItemSnapshotProvider::validateId);
    }

    public ItemSnapshotProvider(Set<String> itemIds) {
        this(itemIds.hashCode() & 0xffffffffL, 0, "<item-registry>", itemIds);
    }

    /** UTF-8, one canonical ID per line; blank lines and # comments are accepted. */
    public static ItemSnapshotProvider read(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        Set<String> ids = new HashSet<>();
        for (int index = 0; index < lines.size(); index++) {
            String id = lines.get(index).replace("\uFEFF", "").trim();
            if (id.isEmpty() || id.startsWith("#")) continue;
            try {
                validateId(id);
                ids.add(id);
            } catch (IllegalArgumentException invalid) {
                throw new IOException(path + ":" + (index + 1) + ": " + invalid.getMessage(), invalid);
            }
        }
        if (ids.isEmpty()) throw new IOException("Item catalog is empty: " + path);
        return new ItemSnapshotProvider(ids.hashCode() & 0xffffffffL, 1,
                path.toAbsolutePath().normalize().toString(), ids);
    }

    @Override
    public List<ContentDescriptor> descriptors() {
        return itemIds.stream().sorted().map(id -> ContentDescriptor.simple(new ContentResourceId(id),
                ContentResourceKind.ITEM, SourceSpans.file(sourceId, ""), List.of())).toList();
    }

    private static void validateId(String id) {
        if (!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Invalid canonical item ID: " + id);
        }
    }
}
