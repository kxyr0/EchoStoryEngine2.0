package io.github.kxyr0.echotrigger.forge.domain;

import io.github.kxyr0.echoevents.api.subscription.EventSubscription;
import io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.UUID;

public final class EventTriggerArchive {
    private final Path root;
    private final EventTriggerDefinitionCodec codec = new EventTriggerDefinitionCodec();

    public EventTriggerArchive(Path root) { this.root = root.toAbsolutePath().normalize(); }

    public void save(EventTriggerDefinition definition) {
        Path path = path(definition.subscription());
        if (Files.exists(path)) {
            if (!find(definition.subscription()).orElseThrow().equals(definition)) {
                throw new IllegalArgumentException("Event subscription revision collision");
            }
            return;
        }
        try {
            Files.createDirectories(root);
            Path temporary = path.resolveSibling(path.getFileName() + ".writing");
            write(temporary, definition);
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    private void write(Path path, EventTriggerDefinition definition) throws IOException {
        try (var channel = FileChannel.open(path, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            var output = new DataOutputStream(Channels.newOutputStream(channel));
            codec.write(output, definition);
            output.flush();
            channel.force(true);
        }
    }

    public Optional<EventTriggerDefinition> find(EventSubscription expected) {
        Path path = path(expected);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try (var input = new DataInputStream(Files.newInputStream(path))) {
            var definition = codec.read(input);
            if (!definition.subscription().equals(expected) || input.read() != -1) {
                throw new IOException("Mismatched trigger archive");
            }
            return Optional.of(definition);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    private Path path(EventSubscription subscription) {
        String key = subscription.id().value() + "\u0000" + subscription.revision();
        return root.resolve(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)) + ".ee1");
    }
}
