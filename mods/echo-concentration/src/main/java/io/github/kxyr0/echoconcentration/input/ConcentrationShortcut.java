package io.github.kxyr0.echoconcentration.input;

/** Keeps editor shortcuts separate from the plain concentration key. */
public final class ConcentrationShortcut {
    private static final int SHORTCUT_MODIFIERS = 0x000F;

    private ConcentrationShortcut() {
    }

    public static boolean accepts(int modifiers) {
        // GLFW Shift, Control, Alt and Super; lock-key flags do not change the shortcut.
        return (modifiers & SHORTCUT_MODIFIERS) == 0;
    }
}
