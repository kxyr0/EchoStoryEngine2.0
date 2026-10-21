package io.github.kxyr0.echodialogue.api.visual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SpeakerVisualRefTest {
    @Test
    void acceptsBuiltInVisualReferences() {
        assertEquals(SpeakerVisualKind.PORTRAIT, SpeakerVisualRef.portrait("sample:merchant").kind());
        assertEquals("sample:father", SpeakerVisualRef.skinHead("sample:father").id().toString());
    }

    @Test
    void rejectsPathsUrlsAndFileExtensions() {
        assertThrows(IllegalArgumentException.class, () -> SpeakerVisualId.parse("Sample:Father"));
        assertThrows(IllegalArgumentException.class, () -> SpeakerVisualId.parse("sample:father.png"));
        assertThrows(IllegalArgumentException.class, () -> SpeakerVisualId.parse("http://skin"));
        assertThrows(IllegalArgumentException.class, () -> SpeakerVisualId.parse("sample:../skin"));
    }

    @Test
    void speakerNameColorsAcceptRgbAndOpaqueRgbaHex() {
        assertEquals(0x8FD8FF, SpeakerNameColor.parse("#8fd8ff").rgb());
        assertEquals("#8FD8FF", SpeakerNameColor.parse("0x8FD8FF").hex());
        assertEquals("#9227CF", SpeakerNameColor.parse("#9227cfff").hex());
        assertThrows(IllegalArgumentException.class, () -> SpeakerNameColor.parse("#12345"));
        assertThrows(IllegalArgumentException.class, () -> SpeakerNameColor.parse("#8fd8ff80"));
        assertThrows(IllegalArgumentException.class, () -> SpeakerNameColor.rgb(0xFF8FD8FF));
    }
}
