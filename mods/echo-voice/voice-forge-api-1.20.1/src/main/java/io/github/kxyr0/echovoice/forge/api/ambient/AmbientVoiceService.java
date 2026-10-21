package io.github.kxyr0.echovoice.forge.api.ambient;

import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import java.util.Objects;

/** Low-priority positional lane that shares Echo Voice's catalog, decoder and PCM cache. */
public interface AmbientVoiceService {
    Result play(SpatialRequest request);
    void stop(String playbackToken);
    int activeCount();

    record SpatialRequest(String playbackToken, VoiceAssetKey assetKey, double x, double y, double z,
            double maximumDistance, double volume) {
        public SpatialRequest {
            playbackToken = Objects.requireNonNull(playbackToken, "playbackToken").trim();
            assetKey = Objects.requireNonNull(assetKey, "assetKey");
            if (playbackToken.isEmpty() || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || maximumDistance <= 0.0D || maximumDistance > 64.0D || volume < 0.0D || volume > 1.0D) {
                throw new IllegalArgumentException("Invalid ambient spatial voice request.");
            }
        }
    }

    record Result(Status status, String detail) {
        public Result { status = Objects.requireNonNull(status, "status"); detail = Objects.requireNonNull(detail); }
    }

    enum Status { QUEUED, STARTED, NO_ASSET, OUT_OF_RANGE, FOREGROUND_PRIORITY, CONCURRENCY_LIMIT, FAILED }
}
