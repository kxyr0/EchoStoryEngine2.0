package io.github.kxyr0.echoanimation.forge.network;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.graph.definition.ChannelCompositionPolicy;
import io.github.kxyr0.echoanimation.api.graph.definition.PlaybackMode;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.api.sync.AnimationChannelSyncState;
import io.github.kxyr0.echoanimation.api.sync.AnimationSubjectSyncSnapshot;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncRevision;
import io.github.kxyr0.echoanimation.forge.face.BlinkTimingOverride;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class AnimationNetworkCodecTest {
    @Test
    void snapshotPacketRoundTripsCompositionAndTransitionFields() {
        ClientboundAnimationSubjectSnapshotPacket original = new ClientboundAnimationSubjectSnapshotPacket(
                snapshot(), true);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        ClientboundAnimationSubjectSnapshotPacket.encode(original, buffer);
        ClientboundAnimationSubjectSnapshotPacket decoded = ClientboundAnimationSubjectSnapshotPacket.decode(buffer);

        Assertions.assertTrue(decoded.full());
        AnimationChannelSyncState channel = decoded.snapshot().channels().get(0);
        Assertions.assertEquals(new AnimationSubjectId("jdh:actor"), decoded.snapshot().subjectId());
        Assertions.assertEquals(77L, decoded.snapshot().authoritativeTick());
        Assertions.assertEquals(12L, decoded.snapshot().syncRevision().value());
        Assertions.assertEquals(18L, channel.elapsedTicks());
        Assertions.assertEquals(40L, channel.durationTicks());
        Assertions.assertEquals(PlaybackMode.LOOP, channel.playbackMode());
        Assertions.assertEquals(1.5D, channel.playbackSpeed(), 0.0001D);
        Assertions.assertEquals(ChannelCompositionPolicy.ADDITIVE, channel.compositionPolicy());
        Assertions.assertEquals(Set.of(BoneId.parse("left_forearm"), BoneId.parse("right_forearm")),
                channel.boneMask());
        Assertions.assertEquals(0.25D, channel.weight(), 0.0001D);
        Assertions.assertEquals(Optional.of(AnimationClipId.parse("jdh:idle")), channel.previousClipId());
        Assertions.assertEquals(6L, channel.transitionElapsedTicks());
        Assertions.assertEquals(10L, channel.transitionDurationTicks());
        Assertions.assertEquals(0.6D, channel.transitionProgress(), 0.0001D);
    }

    @Test
    void blinkTimingPacketRoundTripsSetAndClear() {
        ClientboundBlinkTimingCommandPacket set = ClientboundBlinkTimingCommandPacket.set(
                "jdh:sasha", new BlinkTimingOverride(34, 5, 8, 9));
        FriendlyByteBuf setBuffer = new FriendlyByteBuf(Unpooled.buffer());

        ClientboundBlinkTimingCommandPacket.encode(set, setBuffer);
        ClientboundBlinkTimingCommandPacket decodedSet =
                ClientboundBlinkTimingCommandPacket.decode(setBuffer);

        Assertions.assertFalse(decodedSet.clear());
        Assertions.assertEquals("jdh:sasha", decodedSet.subjectId());
        Assertions.assertEquals(34, decodedSet.periodTicks());
        Assertions.assertEquals(5, decodedSet.closeTicks());
        Assertions.assertEquals(8, decodedSet.holdTicks());
        Assertions.assertEquals(9, decodedSet.openTicks());

        ClientboundBlinkTimingCommandPacket clear = ClientboundBlinkTimingCommandPacket.clear("jdh:sasha");
        FriendlyByteBuf clearBuffer = new FriendlyByteBuf(Unpooled.buffer());

        ClientboundBlinkTimingCommandPacket.encode(clear, clearBuffer);
        ClientboundBlinkTimingCommandPacket decodedClear =
                ClientboundBlinkTimingCommandPacket.decode(clearBuffer);

        Assertions.assertTrue(decodedClear.clear());
        Assertions.assertEquals("jdh:sasha", decodedClear.subjectId());
    }

    @Test
    void faceAnimationClearPacketRoundTripsSubjectAndAllModes() {
        ClientboundFaceAnimationClearPacket subject = ClientboundFaceAnimationClearPacket.subject("jdh:sasha");
        FriendlyByteBuf subjectBuffer = new FriendlyByteBuf(Unpooled.buffer());

        ClientboundFaceAnimationClearPacket.encode(subject, subjectBuffer);
        ClientboundFaceAnimationClearPacket decodedSubject =
                ClientboundFaceAnimationClearPacket.decode(subjectBuffer);

        Assertions.assertFalse(decodedSubject.clearAll());
        Assertions.assertEquals("jdh:sasha", decodedSubject.subjectId());

        ClientboundFaceAnimationClearPacket all = ClientboundFaceAnimationClearPacket.all();
        FriendlyByteBuf allBuffer = new FriendlyByteBuf(Unpooled.buffer());

        ClientboundFaceAnimationClearPacket.encode(all, allBuffer);
        ClientboundFaceAnimationClearPacket decodedAll = ClientboundFaceAnimationClearPacket.decode(allBuffer);

        Assertions.assertTrue(decodedAll.clearAll());
        Assertions.assertEquals("", decodedAll.subjectId());
    }

    private AnimationSubjectSyncSnapshot snapshot() {
        return new AnimationSubjectSyncSnapshot(new AnimationSubjectId("jdh:actor"), 4L, 77L,
                new AnimationSyncRevision(12L), List.of(new AnimationChannelSyncState(
                        new AnimationChannelId("body"),
                        new AnimationStateId("body/wave"),
                        Optional.of(AnimationClipId.parse("jdh:wave")),
                        Optional.of("wave_loop"),
                        Optional.empty(),
                        9L,
                        18L,
                        40L,
                        0.42D,
                        3L,
                        false,
                        false,
                        PlaybackMode.LOOP,
                        1.5D,
                        20,
                        ChannelCompositionPolicy.ADDITIVE,
                        Set.of(BoneId.parse("right_forearm"), BoneId.parse("left_forearm")),
                        0.25D,
                        Optional.of(AnimationClipId.parse("jdh:idle")),
                        6L,
                        10L,
                        0.6D)));
    }
}
