package io.github.kxyr0.echoitemattachments.animation;

import io.github.kxyr0.echoanimation.api.math.Matrix4Transform;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.rig.AnimationAttachmentSocketId;
import io.github.kxyr0.echoanimation.forge.api.AnimationServiceBridge;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.forge.api.anchor.AttachmentAnchorSample;
import io.github.kxyr0.echoitemattachments.forge.api.anchor.AttachmentAnchorSampleEvent;
import io.github.kxyr0.echoitemattachments.forge.api.subject.AttachmentSubjectBridge;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

final class AnimationSocketAnchorProvider {
    private final AnimationSubjectMapping subjects = new AnimationSubjectMapping();
    private final ActivePlaybackBindings bindings;

    AnimationSocketAnchorProvider(ActivePlaybackBindings bindings) {
        this.bindings = bindings;
    }

    void sample(AttachmentAnchorSampleEvent event) {
        var access = AttachmentSubjectBridge.lookup(event.server(), event.subject()).orElse(null);
        if (access == null) return;
        LivingEntity entity = access.entity();
        var animations = AnimationServiceBridge.lookup(event.server());
        if (animations.status()
                == io.github.kxyr0.echoanimation.forge.api.AnimationServiceLookupResult.Status.RESOLVED) {
            var socket = AnimationAttachmentSocketId.parse(event.point().equals(AttachmentPointId.RIGHT_HAND)
                    ? "main_hand" : "off_hand");
            var playback = bindings.playback(event.subject()).map(
                    io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId::parse);
            var sample = animations.service().orElseThrow()
                    .sampleAttachmentSocket(subjects.animation(event.subject()), playback, socket);
            if (sample.isPresent()) {
                event.register(world(entity, sample.orElseThrow().modelTransform(),
                        sample.orElseThrow().serverTick(), sample.orElseThrow().playbackRevision()));
                return;
            }
            if (playback.isPresent()) return;
        }
        event.register(fallback(entity, event.point(), entity.level().getGameTime()));
    }

    private AttachmentAnchorSample world(LivingEntity entity, Matrix4Transform matrix,
            long serverTick, long revision) {
        Vec3 origin = matrix.transformPoint(Vec3.ZERO);
        Vec3 forwardPoint = matrix.transformPoint(new Vec3(0.0D, 0.0D, -1.0D));
        Vec3 upPoint = matrix.transformPoint(new Vec3(0.0D, -1.0D, 0.0D));
        net.minecraft.world.phys.Vec3 position = worldPoint(entity, origin);
        net.minecraft.world.phys.Vec3 forward = worldDirection(entity, forwardPoint.minus(origin));
        net.minecraft.world.phys.Vec3 up = worldDirection(entity, upPoint.minus(origin));
        return new AttachmentAnchorSample(position, forward, up, serverTick, revision);
    }

    private net.minecraft.world.phys.Vec3 worldPoint(LivingEntity entity, Vec3 model) {
        net.minecraft.world.phys.Vec3 local = new net.minecraft.world.phys.Vec3(
                -model.x() / 16.0D, 1.5D - model.y() / 16.0D, model.z() / 16.0D);
        return entity.position().add(rotate(local, 180.0F - entity.yBodyRot));
    }

    private net.minecraft.world.phys.Vec3 worldDirection(LivingEntity entity, Vec3 model) {
        return rotate(new net.minecraft.world.phys.Vec3(-model.x(), -model.y(), model.z()),
                180.0F - entity.yBodyRot).normalize();
    }

    private net.minecraft.world.phys.Vec3 rotate(net.minecraft.world.phys.Vec3 value, float degrees) {
        double radians = degrees * Mth.DEG_TO_RAD;
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return new net.minecraft.world.phys.Vec3(value.x * cosine + value.z * sine,
                value.y, value.z * cosine - value.x * sine);
    }

    private AttachmentAnchorSample fallback(
            LivingEntity entity, AttachmentPointId point, long tick) {
        float yaw = entity.yBodyRot * Mth.DEG_TO_RAD;
        net.minecraft.world.phys.Vec3 forward = new net.minecraft.world.phys.Vec3(
                -Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        net.minecraft.world.phys.Vec3 right = new net.minecraft.world.phys.Vec3(
                forward.z, 0.0D, -forward.x);
        double side = point.equals(AttachmentPointId.RIGHT_HAND) ? 0.35D : -0.35D;
        var position = entity.position().add(0.0D, entity.getBbHeight() * 0.62D, 0.0D)
                .add(right.scale(side)).add(forward.scale(0.08D));
        return new AttachmentAnchorSample(position, forward,
                new net.minecraft.world.phys.Vec3(0.0D, 1.0D, 0.0D), tick, tick);
    }
}
