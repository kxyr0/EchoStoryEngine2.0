package io.github.kxyr0.echoplayer.api.profile;

import io.github.kxyr0.echoplayer.api.appearance.PlayerAppearanceSnapshot;
import io.github.kxyr0.echoplayer.api.appearance.PlayerBaseSkin;
import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.api.identity.PlayerIdentitySnapshot;
import io.github.kxyr0.echoplayer.api.spark.PlayerSparkSnapshot;
import java.util.Objects;

/**
 * Complete immutable durable profile snapshot.
 */
public record PlayerProfileSnapshot(
        EchoPlayerId playerId,
        PlayerIdentitySnapshot identity,
        PlayerSparkSnapshot spark,
        PlayerAppearanceSnapshot appearance,
        PlayerProfileTemplateId templateId,
        String templateFingerprint,
        PlayerProfileLifecycleMetadata lifecycle,
        PlayerProfileRevision profileRevision,
        long repositoryRevision
) {
    public PlayerProfileSnapshot {
        playerId = Objects.requireNonNull(playerId, "playerId");
        identity = Objects.requireNonNull(identity, "identity");
        spark = Objects.requireNonNull(spark, "spark");
        appearance = PlayerBaseSkin.normalize(Objects.requireNonNull(appearance, "appearance"));
        templateId = Objects.requireNonNull(templateId, "templateId");
        templateFingerprint = Objects.requireNonNull(templateFingerprint, "templateFingerprint");
        lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        profileRevision = Objects.requireNonNull(profileRevision, "profileRevision");
        if (!playerId.equals(identity.playerId())) {
            throw new IllegalArgumentException("Profile player ID must match identity player ID.");
        }
        if (repositoryRevision < 0L) {
            throw new IllegalArgumentException("Repository revision must not be negative.");
        }
    }

    public PlayerProfileSnapshot withRevision(PlayerProfileRevision revision, long repositoryRevision) {
        return new PlayerProfileSnapshot(playerId, identity, spark, appearance, templateId,
                templateFingerprint, lifecycle, revision, repositoryRevision);
    }
}
