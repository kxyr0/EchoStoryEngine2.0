package io.github.kxyr0.echonpc.forge.api.client.attachment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class NpcVisualAttachmentTransformProviderTest {
    @Test
    void legacyLambdaProvidersRemainPartRelative() {
        NpcVisualAttachmentTransformProvider provider =
                (anchor, poses) -> {
                };

        assertEquals(
                NpcVisualAttachmentTransformSpace.PART_RELATIVE,
                provider.space(NpcVisualAttachmentAnchor.TORSO_BACK));
    }
}
