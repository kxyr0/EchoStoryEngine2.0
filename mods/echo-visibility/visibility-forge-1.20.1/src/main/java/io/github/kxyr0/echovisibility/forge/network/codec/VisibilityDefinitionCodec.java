package io.github.kxyr0.echovisibility.forge.network.codec;

import io.github.kxyr0.echovisibility.api.geometry.VisibilityAabb;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityDimensionId;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityVector3;
import io.github.kxyr0.echovisibility.api.identity.VisibilityDefinitionRevision;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.api.model.VisibilityWatchDefinition;
import io.github.kxyr0.echovisibility.api.policy.VisibilityConcealmentPolicy;
import io.github.kxyr0.echovisibility.api.policy.VisibilityDetectionMode;
import io.github.kxyr0.echovisibility.api.policy.VisibilityEvaluationRate;
import io.github.kxyr0.echovisibility.api.policy.VisibilityHysteresis;
import io.github.kxyr0.echovisibility.api.policy.VisibilityOcclusionMode;
import io.github.kxyr0.echovisibility.api.policy.VisibilityViewpoint;
import io.github.kxyr0.echovisibility.api.sample.VisibilitySampleKind;
import io.github.kxyr0.echovisibility.api.sample.VisibilitySamplePolicy;
import io.github.kxyr0.echovisibility.api.target.VisibilityBoxTarget;
import io.github.kxyr0.echovisibility.api.target.VisibilityNpcTarget;
import io.github.kxyr0.echovisibility.api.target.VisibilityTarget;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

public final class VisibilityDefinitionCodec {
    private VisibilityDefinitionCodec() {
    }

    public static void write(FriendlyByteBuf buffer, VisibilityWatchDefinition value) {
        buffer.writeUtf(value.id().value());
        buffer.writeLong(value.revision().value());
        writeTarget(buffer, value.target());
        buffer.writeEnum(value.viewpoint());
        buffer.writeEnum(value.detectionMode());
        buffer.writeEnum(value.occlusionMode());
        buffer.writeVarInt(value.samplePolicy().kinds().size());
        value.samplePolicy().kinds().forEach(buffer::writeEnum);
        buffer.writeVarInt(value.samplePolicy().maximumSamples());
        buffer.writeVarInt(value.hysteresis().lostFrames());
        buffer.writeVarInt(value.hysteresis().seenFrames());
        buffer.writeBoolean(value.requireVisibleFirst());
        buffer.writeVarInt(value.edgeMarginPixels());
        buffer.writeEnum(value.concealmentPolicy());
        buffer.writeEnum(value.evaluationRate());
        buffer.writeUtf(value.fingerprint());
    }

    public static VisibilityWatchDefinition read(FriendlyByteBuf buffer) {
        VisibilityWatchId id = new VisibilityWatchId(buffer.readUtf());
        VisibilityDefinitionRevision revision = new VisibilityDefinitionRevision(buffer.readLong());
        VisibilityTarget target = readTarget(buffer);
        VisibilityViewpoint viewpoint = buffer.readEnum(VisibilityViewpoint.class);
        VisibilityDetectionMode detection = buffer.readEnum(VisibilityDetectionMode.class);
        VisibilityOcclusionMode occlusion = buffer.readEnum(VisibilityOcclusionMode.class);
        int count = buffer.readVarInt();
        List<VisibilitySampleKind> samples = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            samples.add(buffer.readEnum(VisibilitySampleKind.class));
        }
        VisibilitySamplePolicy samplePolicy = new VisibilitySamplePolicy(samples, buffer.readVarInt());
        VisibilityHysteresis hysteresis = new VisibilityHysteresis(buffer.readVarInt(), buffer.readVarInt());
        boolean requireVisibleFirst = buffer.readBoolean();
        int margin = buffer.readVarInt();
        VisibilityConcealmentPolicy concealment = buffer.readEnum(VisibilityConcealmentPolicy.class);
        VisibilityEvaluationRate evaluation = buffer.readEnum(VisibilityEvaluationRate.class);
        String fingerprint = buffer.readUtf();
        return new VisibilityWatchDefinition(id, revision, target, viewpoint, detection, occlusion,
                samplePolicy, hysteresis, requireVisibleFirst, margin, concealment, evaluation, fingerprint);
    }

    private static void writeTarget(FriendlyByteBuf buffer, VisibilityTarget target) {
        if (target instanceof VisibilityNpcTarget npc) {
            buffer.writeBoolean(true);
            buffer.writeUtf(npc.npcId());
            buffer.writeDouble(npc.boundsPadding());
            return;
        }
        VisibilityBoxTarget box = (VisibilityBoxTarget) target;
        buffer.writeBoolean(false);
        buffer.writeUtf(box.dimension().value());
        VisibilityGeometryCodec.writeVector(buffer, box.bounds().min());
        VisibilityGeometryCodec.writeVector(buffer, box.bounds().max());
    }

    private static VisibilityTarget readTarget(FriendlyByteBuf buffer) {
        if (buffer.readBoolean()) {
            return new VisibilityNpcTarget(buffer.readUtf(), buffer.readDouble());
        }
        VisibilityDimensionId dimension = new VisibilityDimensionId(buffer.readUtf());
        VisibilityVector3 min = VisibilityGeometryCodec.readVector(buffer);
        VisibilityVector3 max = VisibilityGeometryCodec.readVector(buffer);
        return new VisibilityBoxTarget(dimension, new VisibilityAabb(min, max));
    }
}
