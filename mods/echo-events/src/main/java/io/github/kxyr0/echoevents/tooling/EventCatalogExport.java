package io.github.kxyr0.echoevents.tooling;

import io.github.kxyr0.echoevents.api.catalog.EventCatalogDescriptor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/** Exports the runtime registry for the existing authoring extension. */
public final class EventCatalogExport {
    private EventCatalogExport() { }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) throw new IllegalArgumentException("Expected output JSON path");
        var entries = EventCatalogDescriptor.catalog().stream().map(EventCatalogExport::json)
                .collect(Collectors.joining(",\n"));
        Files.writeString(Path.of(arguments[0]), "[\n" + entries + "\n]\n", StandardCharsets.UTF_8);
    }

    private static String json(EventCatalogDescriptor descriptor) {
        var kind = descriptor.kind();
        return "  {\"predicate\":\"" + kind.predicate() + "\",\"kind\":\"" + kind.stableId()
                + "\",\"durable\":" + kind.durable() + ",\"scope\":\"" + descriptor.scope()
                + "\",\"filters\":[" + descriptor.filterFields().stream()
                        .map(value -> "\"" + value + "\"").collect(Collectors.joining(",")) + "]}";
    }
}
