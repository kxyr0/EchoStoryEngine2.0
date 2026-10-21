package io.github.kxyr0.echovisibility.forge.network.codec;

import io.github.kxyr0.echovisibility.api.identity.VisibilityConcealmentLeaseId;
import io.github.kxyr0.echovisibility.api.identity.VisibilityDefinitionRevision;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchGeneration;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.api.report.VisibilityReport;
import io.github.kxyr0.echovisibility.api.report.VisibilityTransition;
import io.github.kxyr0.echovisibility.api.state.VisibilitySampleResult;
import io.github.kxyr0.echovisibility.api.state.VisibilityState;
import io.github.kxyr0.echovisibility.api.state.VisibilityTransitionKind;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;

public final class VisibilityReportCodec {
    private VisibilityReportCodec() {
    }

    public static void write(FriendlyByteBuf buffer, VisibilityReport value) {
        buffer.writeUtf(value.watchId().value());
        buffer.writeLong(value.generation().value());
        buffer.writeLong(value.targetRevision().value());
        buffer.writeLong(value.connectionGeneration());
        buffer.writeLong(value.reportSequence());
        buffer.writeLong(value.renderedFrame());
        buffer.writeLong(value.clientTick());
        buffer.writeEnum(value.state());
        writeTransition(buffer, value.transition());
        VisibilityGeometryCodec.writeCamera(buffer, value.camera());
        writeSamples(buffer, value.samples());
        buffer.writeBoolean(value.concealmentLeaseId().isPresent());
        value.concealmentLeaseId().ifPresent(id -> buffer.writeUtf(id.value()));
    }

    public static VisibilityReport read(FriendlyByteBuf buffer) {
        VisibilityWatchId watchId = new VisibilityWatchId(buffer.readUtf());
        VisibilityWatchGeneration generation = new VisibilityWatchGeneration(buffer.readLong());
        VisibilityDefinitionRevision revision = new VisibilityDefinitionRevision(buffer.readLong());
        long connection = buffer.readLong();
        long sequence = buffer.readLong();
        long frame = buffer.readLong();
        long clientTick = buffer.readLong();
        VisibilityState state = buffer.readEnum(VisibilityState.class);
        VisibilityTransition transition = readTransition(buffer);
        var camera = VisibilityGeometryCodec.readCamera(buffer);
        VisibilitySampleResult samples = readSamples(buffer);
        Optional<VisibilityConcealmentLeaseId> lease = buffer.readBoolean()
                ? Optional.of(new VisibilityConcealmentLeaseId(buffer.readUtf())) : Optional.empty();
        return new VisibilityReport(watchId, generation, revision, connection, sequence,
                frame, clientTick, state, transition, camera, samples, lease);
    }

    private static void writeTransition(FriendlyByteBuf buffer, VisibilityTransition value) {
        buffer.writeEnum(value.kind());
        buffer.writeEnum(value.previous());
        buffer.writeEnum(value.current());
        buffer.writeLong(value.previousStableFrame());
        buffer.writeLong(value.renderedFrame());
    }

    private static VisibilityTransition readTransition(FriendlyByteBuf buffer) {
        return new VisibilityTransition(buffer.readEnum(VisibilityTransitionKind.class),
                buffer.readEnum(VisibilityState.class), buffer.readEnum(VisibilityState.class),
                buffer.readLong(), buffer.readLong());
    }

    private static void writeSamples(FriendlyByteBuf buffer, VisibilitySampleResult value) {
        buffer.writeVarInt(value.totalSamples());
        buffer.writeVarInt(value.viewportSamples());
        buffer.writeVarInt(value.visibleSamples());
        buffer.writeVarInt(value.occludedSamples());
        buffer.writeVarInt(value.uncertainSamples());
        buffer.writeUtf(value.fingerprint());
    }

    private static VisibilitySampleResult readSamples(FriendlyByteBuf buffer) {
        return new VisibilitySampleResult(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf());
    }
}
