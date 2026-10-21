package io.github.kxyr0.echovisibility.api.state;

public record VisibilitySampleResult(
        int totalSamples,
        int viewportSamples,
        int visibleSamples,
        int occludedSamples,
        int uncertainSamples,
        String fingerprint) {
    public VisibilitySampleResult {
        if (negative(totalSamples, viewportSamples, visibleSamples, occludedSamples, uncertainSamples)
                || viewportSamples > totalSamples || classified(visibleSamples, occludedSamples,
                        uncertainSamples) > viewportSamples) {
            throw new IllegalArgumentException("Visibility sample counters are inconsistent.");
        }
        if (fingerprint == null || fingerprint.isBlank() || fingerprint.length() > 128) {
            throw new IllegalArgumentException("Visibility sample fingerprint is invalid.");
        }
    }

    private static boolean negative(int... values) {
        for (int value : values) {
            if (value < 0) {
                return true;
            }
        }
        return false;
    }

    private static int classified(int visible, int occluded, int uncertain) {
        return visible + occluded + uncertain;
    }
}
