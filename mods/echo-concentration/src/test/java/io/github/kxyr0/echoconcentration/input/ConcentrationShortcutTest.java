package io.github.kxyr0.echoconcentration.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConcentrationShortcutTest {
    @Test
    void undoAndModifiedEditorShortcutsDoNotToggleConcentration() {
        assertFalse(ConcentrationShortcut.accepts(0x0002)); // Ctrl+Z: undo
        assertFalse(ConcentrationShortcut.accepts(0x0003)); // Ctrl+Shift+Z: redo
        assertFalse(ConcentrationShortcut.accepts(0x0004)); // Alt+Z
        assertFalse(ConcentrationShortcut.accepts(0x0008)); // Super+Z
        assertFalse(ConcentrationShortcut.accepts(0x0012)); // Ctrl+Z with Caps Lock
    }

    @Test
    void plainToggleStillWorksWithLockKeysEnabled() {
        assertTrue(ConcentrationShortcut.accepts(0));
        assertTrue(ConcentrationShortcut.accepts(0x0010));
        assertTrue(ConcentrationShortcut.accepts(0x0020));
        assertTrue(ConcentrationShortcut.accepts(0x0030));
    }
}
