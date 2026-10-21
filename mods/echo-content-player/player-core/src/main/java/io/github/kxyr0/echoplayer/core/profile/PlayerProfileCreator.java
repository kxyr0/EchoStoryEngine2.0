package io.github.kxyr0.echoplayer.core.profile;

import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.api.identity.PlayerIdentitySnapshot;
import io.github.kxyr0.echoplayer.api.identity.PlayerNameMode;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileLifecycleMetadata;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileRevision;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import io.github.kxyr0.echoplayer.api.spark.PlayerSparkSnapshot;
import io.github.kxyr0.echoplayer.core.template.PlayerProfileTemplate;
import java.time.Instant;
import java.util.UUID;

/**
 * Creates the first durable profile from the default template.
 */
public final class PlayerProfileCreator {
    public PlayerProfileSnapshot create(UUID accountUuid, String accountName, PlayerProfileTemplate template,
                                        Instant now) {
        EchoPlayerId playerId = EchoPlayerId.fromAccountUuid(accountUuid);
        PlayerIdentitySnapshot identity = template.nameMode() == PlayerNameMode.ACCOUNT
                ? PlayerIdentitySnapshot.account(playerId, accountName)
                : PlayerIdentitySnapshot.account(playerId, accountName)
                .withCustomName(template.customDisplayName().orElseThrow());
        return new PlayerProfileSnapshot(playerId, identity, PlayerSparkSnapshot.from(template.spark()),
                template.appearance(), template.id(), template.fingerprint(),
                PlayerProfileLifecycleMetadata.firstJoin(now, accountName),
                PlayerProfileRevision.initial(), 1L);
    }
}
