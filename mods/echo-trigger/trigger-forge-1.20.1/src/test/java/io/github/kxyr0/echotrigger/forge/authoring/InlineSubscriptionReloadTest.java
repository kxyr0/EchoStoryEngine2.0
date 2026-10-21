package io.github.kxyr0.echotrigger.forge.authoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echotrigger.core.definition.TriggerDefinitionCatalog;
import io.github.kxyr0.echotrigger.core.runtime.TriggerRuntimeHost;
import io.github.kxyr0.echotrigger.forge.service.ForgeTriggerServices;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class InlineSubscriptionReloadTest {
    @Test void invalidSecondHandlerKeepsCatalogAndFixedReloadPublishesBoth(@TempDir Path root) throws Exception {
        var empty = new TriggerDefinitionCatalog(List.of());
        var services = new ForgeTriggerServices(empty, new TriggerRuntimeHost(empty));
        var authoring = new ForgeTriggerAuthoringService(services, root);
        var path = root.resolve("story/content-pack/jdh/data/jdh/echo_trigger/triggers/reaction.trigger.es");
        Files.createDirectories(path.getParent());
        String first = """
                When(Dialogue.Completed("jdh:father"), id: "after_father") { Scene.Play("jdh:leaves"); }
                """;
        String second = """
                When(Dialogue.Completed("jdh:mother"), id: "after_mother") { Scene.Play("jdh:leaves"); }
                """;
        write(path, first);
        assertTrue(authoring.reloadCanonicalRoot().successful());
        var original = services.eventSubscriptions().get(0).subscription();
        long revision = services.catalogRevision();
        write(path, first + second.replace("Scene.Play", "Scene.Unknown"));
        assertFalse(authoring.reloadCanonicalRoot().successful());
        assertEquals(revision, services.catalogRevision());
        assertEquals(List.of(original), services.eventSubscriptions().stream()
                .map(value -> value.subscription()).toList());
        write(path, second + first);
        assertTrue(authoring.reloadCanonicalRoot().successful());
        assertEquals(2, services.eventSubscriptions().size());
        assertTrue(services.eventSubscriptions().stream()
                .anyMatch(value -> value.subscription().id().equals(original.id())));
        assertTrue(new TriggerDefinitionCatalog(services.definitions()).runtimeView().definitions().isEmpty());
        Files.delete(path);
        assertTrue(authoring.reloadCanonicalRoot().successful());
        assertTrue(services.eventSubscriptions().isEmpty());
    }

    private void write(Path path, String handlers) throws Exception {
        Files.writeString(path, "class Reaction : TriggerResource { Id(\"jdh:reaction\"); " + handlers + "}");
    }
}
