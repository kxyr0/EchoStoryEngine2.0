package io.github.kxyr0.echoanimation.core.playback;

import io.github.kxyr0.echoanimation.api.graph.definition.ChannelCompositionPolicy;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.math.Matrix4Transform;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Composes sampled channels using authored order and policy only.
 */
public final class ChannelPoseComposition {
    private final PoseBlender blender = new PoseBlender();

    public AnimationPose compose(AnimationPose base, List<SampledChannelPose> channels) {
        Map<BoneId, EulerTransform> result = new LinkedHashMap<>(base.transforms());
        Map<BoneId, Matrix4Transform> matrices = new LinkedHashMap<>(base.matrixTransforms());
        for (SampledChannelPose channel : ordered(channels)) {
            apply(result, matrices, channel);
        }
        return AnimationPose.of(result, matrices);
    }

    private List<SampledChannelPose> ordered(List<SampledChannelPose> channels) {
        return channels.stream()
                .sorted(Comparator.comparingInt((SampledChannelPose value) -> value.channel().visualOrder())
                        .thenComparing(value -> value.channel().id()))
                .toList();
    }

    private void apply(
            Map<BoneId, EulerTransform> target,
            Map<BoneId, Matrix4Transform> matrices,
            SampledChannelPose channel
    ) {
        for (Map.Entry<BoneId, EulerTransform> entry : channel.pose().transforms().entrySet()) {
            if (!owns(channel.channel().boneMask(), entry.getKey())) {
                continue;
            }
            if (channel.channel().compositionPolicy() == ChannelCompositionPolicy.ADDITIVE) {
                if (matrices.containsKey(entry.getKey())) {
                    throw new IllegalArgumentException("Additive Euler channels cannot target a matrix-authored bone.");
                }
                applyAdditive(target, entry.getKey(), entry.getValue(), channel.weight());
            } else if (matrices.containsKey(entry.getKey())) {
                Matrix4Transform current = matrices.get(entry.getKey());
                Matrix4Transform value = Matrix4Transform.fromEuler(entry.getValue());
                matrices.put(entry.getKey(), blender.blend(current, value, channel.weight()));
            } else {
                applyOverride(target, entry.getKey(), entry.getValue(), channel.weight());
            }
        }
        for (Map.Entry<BoneId, Matrix4Transform> entry : channel.pose().matrixTransforms().entrySet()) {
            if (!owns(channel.channel().boneMask(), entry.getKey())) {
                continue;
            }
            if (channel.channel().compositionPolicy() == ChannelCompositionPolicy.ADDITIVE) {
                throw new IllegalArgumentException("Matrix-authored tracks do not support additive composition.");
            }
            Matrix4Transform current = matrices.get(entry.getKey());
            if (current == null) {
                current = Matrix4Transform.fromEuler(target.getOrDefault(entry.getKey(), EulerTransform.IDENTITY));
            }
            matrices.put(entry.getKey(), blender.blend(current, entry.getValue(), channel.weight()));
            target.remove(entry.getKey());
        }
    }

    private boolean owns(Set<BoneId> mask, BoneId boneId) {
        return mask.isEmpty() || mask.contains(boneId);
    }

    private void applyOverride(Map<BoneId, EulerTransform> target, BoneId boneId, EulerTransform value, double weight) {
        EulerTransform current = target.getOrDefault(boneId, EulerTransform.IDENTITY);
        target.put(boneId, blender.blend(current, value, weight));
    }

    private void applyAdditive(Map<BoneId, EulerTransform> target, BoneId boneId, EulerTransform delta, double weight) {
        EulerTransform weighted = blender.blend(EulerTransform.IDENTITY, delta, weight);
        target.put(boneId, target.getOrDefault(boneId, EulerTransform.IDENTITY).plusDelta(weighted));
    }
}
