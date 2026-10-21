package io.github.kxyr0.echostoryengine.story.forge.extension.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.core.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionContext;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionFactory;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.infrastructure.ForgeStoryClock;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ForgeExtensionCatalogBuilderTest {
    private final InMemoryDiagnosticBuffer diagnosticsBuffer = new InMemoryDiagnosticBuffer(16);
    private final ForgeRuntimeDiagnostics diagnostics = new ForgeRuntimeDiagnostics();
    private final ForgeExtensionCatalogBuilder builder = new ForgeExtensionCatalogBuilder(
            diagnosticsBuffer,
            new ForgeStoryClock(),
            diagnostics);

    @Test
    void duplicateExtensionIdsAreRejectedBeforeSessionCreation() {
        ExtensionCatalog catalog = builder.build(List.of(factory("sample:extension", 1), factory("sample:extension",
                1)), null);

        assertEquals(0, catalog.activeActionCount());
        assertTrue(diagnosticsBuffer.recent().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("ESE-FORGE-EXT-006")));
    }

    @Test
    void incompatibleApiVersionIsRejectedBeforeSessionCreation() {
        ExtensionCatalog catalog = builder.build(List.of(factory("sample:extension", 2)), null);

        assertEquals(0, catalog.activeActionCount());
        assertTrue(catalog.detail(NamespacedId.of("sample:extension")).orElseThrow()
                .summary().status().equals(ExtensionStatus.INCOMPATIBLE.name()));
    }

    private ForgeStoryExtensionFactory factory(String extensionId, int requiredMajor) {
        StoryExtensionDescriptor descriptor = new StoryExtensionDescriptor(
                NamespacedId.of(extensionId),
                "Sample",
                "1.0.0",
                requiredMajor,
                0,
                List.of());
        return new ForgeStoryExtensionFactory() {
            @Override
            public StoryExtensionDescriptor descriptor() {
                return descriptor;
            }

            @Override
            public StoryExtensionSession create(ForgeStoryExtensionContext context) {
                throw new AssertionError("session must not be created for rejected extension");
            }
        };
    }
}
