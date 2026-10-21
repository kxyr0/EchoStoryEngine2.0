package io.github.kxyr0.echomagic.forge.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureTemplateTarget;
import io.github.kxyr0.echomagic.core.gesture.MagicGestureMode;
import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;
import io.github.kxyr0.echomagic.core.gesture.template.MagicGestureTemplateLibrary;
import io.github.kxyr0.echomagic.forge.network.gesture.MagicGestureTemplatesPacket;
import io.github.kxyr0.echomagic.forge.network.gesture.MagicGestureTrainingSamplePacket;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class MagicCastIntentPacketTest {
    @Test void roundTripPreservesGestureCoordinatesExactly() {
        MagicGesturePoint point = new MagicGesturePoint(0.123456789012345D, -0.987654321098765D, 417L);
        MagicCastIntentPacket original = new MagicCastIntentPacket(MagicGestureMode.SCHOOL,
                List.of(List.of(point, new MagicGesturePoint(0.75D, 0.04D, 693L))));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        original.encode(buffer);
        MagicGesturePoint decoded = MagicCastIntentPacket.decode(buffer).strokes().get(0).get(0);

        assertEquals(point, decoded);
    }

    @Test void templateAndTrainingPacketsPreservePersonalHandwritingExactly() {
        List<MagicGesturePoint> sample = List.of(
                new MagicGesturePoint(0.123456789012345D, -0.012345678901234D, 0L),
                new MagicGesturePoint(0.876543210987654D, 0.098765432109876D, 350L));
        MagicGestureTemplateLibrary library = new MagicGestureTemplateLibrary();
        var template = library.train(MagicGestureTemplateTarget.LINE, List.of(sample, sample, sample));
        FriendlyByteBuf templateBuffer = new FriendlyByteBuf(Unpooled.buffer());
        new MagicGestureTemplatesPacket(Map.of(MagicGestureTemplateTarget.LINE, template))
                .encode(templateBuffer);

        assertEquals(template, MagicGestureTemplatesPacket.decode(templateBuffer).templates()
                .get(MagicGestureTemplateTarget.LINE));

        FriendlyByteBuf sampleBuffer = new FriendlyByteBuf(Unpooled.buffer());
        new MagicGestureTrainingSamplePacket(MagicGestureTemplateTarget.LINE, sample).encode(sampleBuffer);
        assertEquals(sample, MagicGestureTrainingSamplePacket.decode(sampleBuffer).points());
    }
}
