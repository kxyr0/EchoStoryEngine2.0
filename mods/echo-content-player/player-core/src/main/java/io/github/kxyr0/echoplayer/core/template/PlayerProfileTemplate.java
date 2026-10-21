package io.github.kxyr0.echoplayer.core.template;

import io.github.kxyr0.echoplayer.api.appearance.PlayerAppearanceSnapshot;
import io.github.kxyr0.echoplayer.api.appearance.PlayerBaseSkin;
import io.github.kxyr0.echoplayer.api.identity.PlayerDisplayName;
import io.github.kxyr0.echoplayer.api.identity.PlayerNameMode;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileTemplateId;
import io.github.kxyr0.echoplayer.api.spark.PlayerSpark;
import java.util.Objects;
import java.util.Optional;

/**
 * Bound default profile template.
 */
public record PlayerProfileTemplate(
        PlayerProfileTemplateId id,
        PlayerNameMode nameMode,
        Optional<PlayerDisplayName> customDisplayName,
        PlayerSpark spark,
        PlayerAppearanceSnapshot appearance,
        String fingerprint
) {
    public PlayerProfileTemplate {
        id = Objects.requireNonNull(id, "id");
        nameMode = Objects.requireNonNull(nameMode, "nameMode");
        customDisplayName = Objects.requireNonNull(customDisplayName, "customDisplayName");
        spark = Objects.requireNonNull(spark, "spark");
        appearance = Objects.requireNonNull(appearance, "appearance");
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        if (nameMode == PlayerNameMode.CUSTOM && customDisplayName.isEmpty()) {
            throw new IllegalArgumentException("CUSTOM template name mode requires custom name.");
        }
    }

    public static PlayerProfileTemplate fallback() {
        return new PlayerProfileTemplate(PlayerProfileTemplateId.parse("jdh:player/default"),
                PlayerNameMode.CUSTOM, Optional.of(PlayerDisplayName.of("Лололошка")),
                PlayerSpark.full(100), PlayerBaseSkin.snapshot(0L), "fallback");
    }
}
