package io.github.kxyr0.echostoryengine.story.extension.api.descriptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class StoryExtensionDescriptorTest {
    @Test
    void descriptorDefensivelyCopiesActions() {
        List<ActionHandlerDescriptor> actions = new ArrayList<>();
        actions.add(new ActionHandlerDescriptor(NamespacedId.of("sample:action"),
                ActionHandlerCapabilities.recoverableAsync()));

        StoryExtensionDescriptor descriptor = new StoryExtensionDescriptor(
                NamespacedId.of("sample:extension"),
                "Sample",
                "1.0.0",
                1,
                0,
                actions);
        actions.clear();

        assertEquals(1, descriptor.actions().size());
        assertThrows(UnsupportedOperationException.class, () -> descriptor.actions().clear());
    }

    @Test
    void compatibilityRequiresExactMajorAndEnoughMinor() {
        StoryExtensionDescriptor descriptor = new StoryExtensionDescriptor(
                NamespacedId.of("sample:extension"),
                "Sample",
                "1.0.0",
                1,
                0,
                List.of());

        assertTrue(descriptor.compatibleWith(new StoryExtensionApiVersion(1, 0)));
        assertTrue(descriptor.compatibleWith(new StoryExtensionApiVersion(1, 2)));
        org.junit.jupiter.api.Assertions.assertFalse(descriptor.compatibleWith(new StoryExtensionApiVersion(2, 0)));
    }
}
