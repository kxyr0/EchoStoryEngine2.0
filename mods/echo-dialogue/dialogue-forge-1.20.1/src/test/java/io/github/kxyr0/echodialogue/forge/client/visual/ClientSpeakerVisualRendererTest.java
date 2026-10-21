package io.github.kxyr0.echodialogue.forge.client.visual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualId;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualKind;
import java.lang.reflect.Method;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ClientSpeakerVisualRendererTest {
    @Test
    void skinHeadOuterLayerInflatesTinyIconByOnePixel() throws ReflectiveOperationException {
        Object bounds = skinHeadOuterBounds(10, 20, 16);

        assertBounds(bounds, 9, 19, 18, 18);
    }

    @Test
    void skinHeadOuterLayerInflatesLargerIconProportionally() throws ReflectiveOperationException {
        Object bounds = skinHeadOuterBounds(10, 20, 40);

        assertBounds(bounds, 7, 17, 46, 46);
    }

    @Test
    void effectiveAlphaIsClampedToRenderableBounds() {
        assertEquals(0.0F, ClientSpeakerVisualRenderer.effectiveAlpha(-0.25F));
        assertEquals(0.55F, ClientSpeakerVisualRenderer.effectiveAlpha(0.55F));
        assertEquals(1.0F, ClientSpeakerVisualRenderer.effectiveAlpha(1.25F));
    }

    @Test
    void portraitSkinSheetRendersAsSkinHead() {
        ClientSpeakerVisual visual = ClientSpeakerVisual.skinSheet(SpeakerVisualKind.PORTRAIT,
                SpeakerVisualId.parse("sample:father"), texture(), 64, 64);

        assertTrue(ClientSpeakerVisualRenderer.rendersAsSkinHead(visual));
    }

    @Test
    void normalPortraitKeepsPortraitRenderer() {
        ClientSpeakerVisual visual = ClientSpeakerVisual.image(SpeakerVisualKind.PORTRAIT,
                SpeakerVisualId.parse("sample:merchant"), texture(), 128, 128);

        assertFalse(ClientSpeakerVisualRenderer.rendersAsSkinHead(visual));
    }

    private static Object skinHeadOuterBounds(int x, int y, int size) throws ReflectiveOperationException {
        Method method = ClientSpeakerVisualRenderer.class
                .getDeclaredMethod("skinHeadOuterBounds", int.class, int.class, int.class);
        method.setAccessible(true);
        return method.invoke(null, x, y, size);
    }

    private static void assertBounds(Object bounds, int x, int y, int width, int height)
            throws ReflectiveOperationException {
        assertEquals(x, value(bounds, "x"));
        assertEquals(y, value(bounds, "y"));
        assertEquals(width, value(bounds, "width"));
        assertEquals(height, value(bounds, "height"));
    }

    private static int value(Object bounds, String methodName) throws ReflectiveOperationException {
        Method method = bounds.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return ((Integer) method.invoke(bounds)).intValue();
    }

    private static ResourceLocation texture() {
        return ResourceLocation.fromNamespaceAndPath("sample", "textures/echo_dialogue/portraits/father.png");
    }
}
