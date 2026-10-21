package io.github.kxyr0.echoitem.api;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EchoItemIdTest {
    @Test
    void validIdParses() {
        EchoItemId id = EchoItemId.parse("jdh:void_sword");

        assertEquals("jdh", id.namespace());
        assertEquals("void_sword", id.path());
        assertEquals("jdh:void_sword", id.value());
    }

    @Test
    void uppercaseIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> EchoItemId.parse("jdh:VoidSword"));
    }

    @Test
    void invalidNamespaceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> EchoItemId.parse("mine craft:void_sword"));
        assertThrows(IllegalArgumentException.class, () -> EchoItemId.parse("minecraft:void_sword"));
    }

    @Test
    void invalidPathIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> EchoItemId.parse("jdh:void sword"));
    }

    @Test
    void filenameMismatchIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> EchoItemId.parseForSource("jdh:void_sword", Path.of("training_dagger.weapon.es")));
    }

    @Test
    void pathTraversalIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> EchoItemId.parse("jdh:../void_sword"));
    }
}
