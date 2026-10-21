package io.github.kxyr0.echomagic.forge.network;

import io.github.kxyr0.echomagic.forge.network.gesture.MagicSchoolSelectedPacket;
import io.github.kxyr0.echomagic.forge.network.gesture.MagicGestureTemplatesPacket;
import io.github.kxyr0.echomagic.forge.network.gesture.MagicGestureTrainingStatusPacket;
import net.minecraftforge.eventbus.api.Event;

public abstract class MagicClientPacketEvent extends Event {
    private MagicClientPacketEvent() { }

    public static final class Impact extends MagicClientPacketEvent {
        private final MagicImpactPacket packet;
        public Impact(MagicImpactPacket packet) { this.packet = packet; }
        public MagicImpactPacket packet() { return packet; }
    }

    public static final class Rejected extends MagicClientPacketEvent {
        private final MagicCastRejectedPacket packet;
        public Rejected(MagicCastRejectedPacket packet) { this.packet = packet; }
        public MagicCastRejectedPacket packet() { return packet; }
    }

    public static final class State extends MagicClientPacketEvent {
        private final MagicPlayerStateSnapshotPacket packet;
        public State(MagicPlayerStateSnapshotPacket packet) { this.packet = packet; }
        public MagicPlayerStateSnapshotPacket packet() { return packet; }
    }

    public static final class SchoolSelected extends MagicClientPacketEvent {
        private final MagicSchoolSelectedPacket packet;
        public SchoolSelected(MagicSchoolSelectedPacket packet) { this.packet = packet; }
        public MagicSchoolSelectedPacket packet() { return packet; }
    }

    public static final class TrainingStatus extends MagicClientPacketEvent {
        private final MagicGestureTrainingStatusPacket packet;
        public TrainingStatus(MagicGestureTrainingStatusPacket packet) { this.packet = packet; }
        public MagicGestureTrainingStatusPacket packet() { return packet; }
    }

    public static final class Templates extends MagicClientPacketEvent {
        private final MagicGestureTemplatesPacket packet;
        public Templates(MagicGestureTemplatesPacket packet) { this.packet = packet; }
        public MagicGestureTemplatesPacket packet() { return packet; }
    }
}
