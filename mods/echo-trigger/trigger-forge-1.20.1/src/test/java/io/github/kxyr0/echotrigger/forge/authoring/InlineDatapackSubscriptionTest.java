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
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class InlineDatapackSubscriptionTest {
    @Test void discoversNativeDatapackProfilesAndKeepsLastGoodCatalog(@TempDir Path root) throws Exception {
        Path pack = root.resolve("pack");
        Path story = write(pack, "echo_story/stories/chapter.es", "StoryResource");
        write(pack, "echo_dialogue/dialogues/mother.dialogue.es", "DialogueResource");
        write(pack, "echo_trigger/triggers/reaction.trigger.es", "TriggerResource");
        var empty = new TriggerDefinitionCatalog(List.of());
        var services = new ForgeTriggerServices(empty, new TriggerRuntimeHost(empty));
        var authoring = new ForgeTriggerAuthoringService(services, root.resolve("game"));
        try (var manager = new MultiPackResourceManager(PackType.SERVER_DATA,
                List.of(new PathPackResources("test", pack, false)))) {
            authoring.bindResources(() -> manager);
            var loaded = authoring.reloadCanonicalRoot();
            assertTrue(loaded.successful(), loaded.diagnostics().toString());
            assertEquals(3, services.eventSubscriptions().size());
            long revision = services.catalogRevision();
            Files.writeString(story, Files.readString(story).replace("Scene.Play", "Scene.Unknown"));
            assertFalse(authoring.reloadCanonicalRoot().successful());
            assertEquals(revision, services.catalogRevision());
            assertEquals(3, services.eventSubscriptions().size());
        }
    }

    private Path write(Path pack, String relative, String profile) throws Exception {
        Path file = pack.resolve("data/jdh/" + relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "class Example : " + profile + " { " + """
                When(Dialogue.Completed("jdh:father"), id: "after_father") { Scene.Play("jdh:leaves"); }
                }
                """);
        return file;
    }
}
