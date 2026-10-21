package io.github.kxyr0.echoseating.forge.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SeatZoneResourceDiscoveryTest {
    @TempDir
    private Path temp;

    @Test
    void discoversInstanceStoryContentPackFromSaveWorldRoot() throws Exception {
        Path minecraft = temp.resolve("minecraft");
        Path zone = minecraft.resolve("story/content-pack/jdh/data/jdh/echo_seating/zones/home/first_morning.es");
        Files.createDirectories(zone.getParent());
        Files.writeString(zone, "class BreakfastTableSeats : SeatZoneResource {}", StandardCharsets.UTF_8);
        Path worldRoot = minecraft.resolve("saves/New World");
        Files.createDirectories(worldRoot);

        var resources = SeatZoneResourceDiscovery.discover(worldRoot);

        assertEquals(1, resources.size());
        assertEquals("jdh/data/jdh/echo_seating/zones/home/first_morning.es", resources.get(0).sourceId());
    }
}
