package io.github.kxyr0.echostoryengine.story.forge.diagnostic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.forge.host.catalog.CatalogReloadStatus;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.CompiledStoryCatalogSnapshot;
import io.github.kxyr0.echostoryengine.story.forge.host.ForgeServerHostState;
import io.github.kxyr0.echostoryengine.story.forge.host.readmodel.HostHealth;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ForgeDiagnosticFormatterTest {
    @Test
    void commandTextIsRussianUtf8WithoutMojibakeMarkers() {
        ForgeDiagnosticFormatter formatter = new ForgeDiagnosticFormatter();
        Instant now = Instant.parse("2026-07-12T06:00:00Z");
        List<String> lines = formatter.health(
                new HostHealth(
                        ForgeServerHostState.ACTIVE,
                        1,
                        1,
                        new CatalogReloadStatus("ACCEPTED", 1, 1, 0, now),
                        1,
                        1,
                        0,
                        64,
                        0,
                        0),
                CompiledStoryCatalogSnapshot.empty(now),
                0);
        String text = String.join("\n", lines);

        assertTrue(text.contains("Состояние Echo Story Engine"));
        assertTrue(text.contains("Ожидающих выполнений"));
        assertTrue(text.contains("Очередь сохранения"));
        assertTrue(text.contains("Недавних предупреждений/ошибок"));
        assertFalse(text.contains("Waiting execution"));
        assertFalse(text.contains("Persistence queue"));
        assertFalse(text.contains("Server work queue"));
        assertFalse(text.contains("Compiled story"));
        assertFalse(text.contains("Рґ"));
        assertFalse(text.contains("Рµ"));
        assertFalse(text.contains("СЃ"));
        assertFalse(text.contains("Рџ"));
    }
}
