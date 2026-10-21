package io.github.kxyr0.echosavemigration.persistence.journal;

import io.github.kxyr0.echosavemigration.api.MigrationId;
import io.github.kxyr0.echosavemigration.api.SaveSchemaVersion;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public final class MigrationJournalStore {
    public void write(Path path, MigrationJournal journal) throws IOException {
        String text = String.join("\n",
                journal.state().name(), encode(journal.source().toString()), encode(journal.candidate().toString()),
                encode(journal.backup().toString()), Integer.toString(journal.from().value()),
                Integer.toString(journal.to().value()), journal.migrations().stream().map(MigrationId::value)
                        .map(this::encode).reduce((a, b) -> a + "," + b).orElse(""),
                journal.candidateHash(), Long.toString(journal.updatedAt().toEpochMilli())) + "\n";
        Files.writeString(path, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) { channel.force(true); }
    }

    public Optional<MigrationJournal> read(Path path) throws IOException {
        if (!Files.exists(path)) return Optional.empty();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.size() != 9) throw new IOException("Invalid migration journal.");
        List<MigrationId> ids = lines.get(6).isEmpty() ? List.of() : java.util.Arrays.stream(lines.get(6).split(","))
                .map(this::decode).map(MigrationId::new).toList();
        return Optional.of(new MigrationJournal(MigrationJournalState.valueOf(lines.get(0)),
                Path.of(decode(lines.get(1))), Path.of(decode(lines.get(2))), Path.of(decode(lines.get(3))),
                new SaveSchemaVersion(Integer.parseInt(lines.get(4))),
                new SaveSchemaVersion(Integer.parseInt(lines.get(5))), ids, lines.get(7),
                Instant.ofEpochMilli(Long.parseLong(lines.get(8)))));
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
