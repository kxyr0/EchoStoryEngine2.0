package io.github.kxyr0.echocontentdoctor.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echocontentdoctor.engine.ContentDoctor;
import io.github.kxyr0.echocontentdoctor.engine.ContentDoctorInput;
import io.github.kxyr0.echocontentdoctor.source.SourceCatalogScanner;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class InlineEventDoctorTest {
    @Test void scansRealStoryDialogueAndTriggerInlineSources(@TempDir Path root) throws Exception {
        String handler = """
                When(Dialogue.Completed("jdh:father"), id: "after_father") { Scene.Play("jdh:leaves"); }
                """;
        write(root, "echo_dialogue/dialogues/father.dialogue.es",
                "class Father : DialogueResource { Node Intro() { Line(\"john\", \"Hi.\"); } }");
        write(root, "echo_dialogue/dialogues/mother.dialogue.es",
                "class Mother : DialogueResource { Node Intro() { Line(\"john\", \"Hi.\"); } " + handler + "}");
        write(root, "echo_story/stories/chapter.es",
                "class Chapter : StoryResource { OnStart() { Story.Set(\"ready\", true); } " + handler + "}");
        write(root, "echo_trigger/triggers/reaction.trigger.es",
                "class Reaction : TriggerResource { Id(\"jdh:reaction\"); " + handler + "}");
        write(root, "echo_scene/scenes/leaves.es", "class Leaves : SceneResource { Scene Start() { Time.Set(\"08:20\"); } }");
        var scan = new SourceCatalogScanner().scan(root);
        assertTrue(scan.diagnostics().isEmpty(), scan.diagnostics().toString());
        assertEquals(3, scan.catalog().resources().stream().mapToLong(value -> value.eventSubscriptions().size()).sum());
        var result = new ContentDoctor().validate(ContentDoctorInput.catalogOnly(scan.catalog()));
        assertEquals(0, result.errors(), result.diagnostics().toString());
        var mother = root.resolve("data/jdh/echo_dialogue/dialogues/mother.dialogue.es");
        Files.writeString(mother, Files.readString(mother).replace("jdh:leaves", "jdh:missing"));
        var broken = new SourceCatalogScanner().scan(root);
        var invalid = new ContentDoctor().validate(ContentDoctorInput.catalogOnly(broken.catalog()));
        assertTrue(invalid.diagnostics().stream().anyMatch(value ->
                value.code().equals("ECHO_DOCTOR_EVENT_ACTION_TARGET_NOT_FOUND")));
    }

    private void write(Path root, String relative, String text) throws Exception {
        Path path = root.resolve("data/jdh/" + relative);
        Files.createDirectories(path.getParent());
        Files.writeString(path, text);
    }
}
