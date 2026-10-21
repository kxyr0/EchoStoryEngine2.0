package io.github.kxyr0.echoitem.core.script;

import io.github.kxyr0.echoitem.api.definition.ItemDefinitionRevision;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentedExamplesTest {
    private final EchoScriptWeaponCompiler compiler = new EchoScriptWeaponCompiler();

    @Test
    void packagedExamplesPassParserAndBinder() throws IOException {
        assertExample("../item-forge-1.20.1/src/main/resources/data/jdh/echo_item/items/weapons/swords/"
                + "void_sword.weapon.es");
        assertExample("../item-forge-1.20.1/src/main/resources/data/jdh/echo_item/items/weapons/daggers/"
                + "training_dagger.weapon.es");
    }

    private void assertExample(String path) throws IOException {
        String text = Files.readString(Path.of(path));
        assertTrue(compiler.compile(path, text, new ItemDefinitionRevision(1L)).successful());
    }
}
