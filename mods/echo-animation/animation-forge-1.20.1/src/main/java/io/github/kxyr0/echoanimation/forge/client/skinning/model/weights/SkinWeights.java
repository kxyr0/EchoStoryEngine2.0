package io.github.kxyr0.echoanimation.forge.client.skinning.model.weights;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Four normalized bone influences ready for a GPU vertex. */
public record SkinWeights(int[] indices, float[] weights, boolean truncated, boolean fallback) {
    public static final int LIMIT = 4;

    public SkinWeights {
        if (indices.length != LIMIT || weights.length != LIMIT) {
            throw new IllegalArgumentException("Skin weights require exactly four slots.");
        }
        indices = indices.clone();
        weights = weights.clone();
    }

    public static SkinWeights of(int[] indices, float[] weights, int rootIndex) {
        if (indices.length != weights.length) {
            throw new IllegalArgumentException("Bone index and weight counts differ.");
        }
        List<Influence> positive = new ArrayList<>();
        for (int index = 0; index < indices.length; index++) {
            float weight = weights[index];
            if (!Float.isFinite(weight) || weight < 0.0F) {
                throw new IllegalArgumentException("Bone weights must be finite and non-negative.");
            }
            if (weight > 0.0F) {
                positive.add(new Influence(indices[index], weight));
            }
        }
        positive.sort(Comparator.comparingDouble(Influence::weight).reversed());
        boolean truncated = positive.size() > LIMIT;
        if (positive.size() > LIMIT) {
            positive = positive.subList(0, LIMIT);
        }
        if (positive.isEmpty()) {
            return rigid(rootIndex, true);
        }
        float sum = (float) positive.stream().mapToDouble(Influence::weight).sum();
        int[] compactIndices = new int[LIMIT];
        float[] compactWeights = new float[LIMIT];
        for (int index = 0; index < positive.size(); index++) {
            compactIndices[index] = positive.get(index).bone();
            compactWeights[index] = positive.get(index).weight() / sum;
        }
        return new SkinWeights(compactIndices, compactWeights, truncated, false);
    }

    public static SkinWeights rigid(int boneIndex, boolean fallback) {
        return new SkinWeights(new int[]{boneIndex, 0, 0, 0}, new float[]{1.0F, 0.0F, 0.0F, 0.0F},
                false, fallback);
    }

    @Override
    public int[] indices() {
        return indices.clone();
    }

    @Override
    public float[] weights() {
        return weights.clone();
    }

    public int index(int slot) {
        return indices[slot];
    }

    public float weight(int slot) {
        return weights[slot];
    }

    private record Influence(int bone, float weight) {
    }
}
