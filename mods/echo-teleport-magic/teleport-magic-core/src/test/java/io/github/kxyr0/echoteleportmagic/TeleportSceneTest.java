package io.github.kxyr0.echoteleportmagic;

import io.github.kxyr0.echoscene.api.model.BlockingMode;
import io.github.kxyr0.echoscene.api.definition.SceneCapabilityKind;
import io.github.kxyr0.echoscene.compile.*;
import io.github.kxyr0.echoscene.compile.pipeline.SceneCompiler;
import io.github.kxyr0.echoscene.compile.source.SceneSourceInput;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TeleportSceneTest {
    private final SceneCompiler compiler = new SceneCompiler(SceneSymbolCatalog.of(List.of(
            new SceneSymbol("Nolan", "jdh:nolan", SceneSymbolKind.NPC),
            new SceneSymbol("John", "jdh:john", SceneSymbolKind.PLAYER),
            new SceneSymbol("ChemistryDoor", "jdh:chemistry_door", SceneSymbolKind.LOCATION))));
    private SceneCompileResult compile(String body) {
        return compiler.compile(new SceneSourceInput("jdh", "teleport.es",
                "class Teleport : SceneResource { Scene Start() { " + body + " } }", 1));
    }
    @Test void realParserAndBinderProduceTypedBlockingActorAndLocationReferences() {
        var result = compile("TeleportMagic.To(Nolan, ChemistryDoor);");
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        var command = result.definition().orElseThrow().stages().get(0).commands().get(0);
        assertEquals(BlockingMode.DURABLE_BLOCKING, command.blockingMode());
        assertEquals(List.of("jdh:nolan", "jdh:chemistry_door"), command.arguments());
        assertEquals(SceneCapabilityKind.NPC, command.capabilities().get(0).kind());
        assertTrue(result.descriptor().isPresent());
    }
    @Test void playerUsesExistingPlayerSymbolAndLease() {
        var result = compile("TeleportMagic.To(John, ChemistryDoor);");
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        assertEquals(SceneCapabilityKind.PLAYER, result.definition().orElseThrow().capabilityPlan().get(0).kind());
    }
    @Test void misspellingHasSourceSpanAndCandidateAndRejectsBeforeExecution() {
        var result = compile("TeleportMagic.To(Nolan, ChemstryDoor);");
        assertFalse(result.successful());
        var diagnostic = result.diagnostics().get(0);
        assertEquals("teleport.es", diagnostic.span().sourceId().value());
        assertEquals(1, diagnostic.span().start().line());
        assertTrue(diagnostic.message().contains("ChemistryDoor"));
    }
    @Test void locationCannotBeUsedAsSubjectAndRawCoordinatesAreNotTheDsl() {
        assertFalse(compile("TeleportMagic.To(ChemistryDoor, ChemistryDoor);").successful());
        assertFalse(compile("TeleportMagic.To(Nolan, 20, 64, 20);").successful());
    }
}
