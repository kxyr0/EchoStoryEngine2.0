package io.github.kxyr0.echoplayer.core.lifecycle;

import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.api.identity.PlayerIdentitySnapshot;
import io.github.kxyr0.echoplayer.api.identity.PlayerNameMode;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import io.github.kxyr0.echoplayer.core.profile.PlayerProfileCreator;
import io.github.kxyr0.echoplayer.core.repository.PlayerProfileRepository;
import io.github.kxyr0.echoplayer.core.template.PlayerProfileTemplate;
import java.time.Instant;
import java.util.UUID;

/**
 * Login/logout checkpoint logic owned by the server thread.
 */
public final class PlayerLifecycleCheckpoint {
    private final PlayerProfileCreator creator = new PlayerProfileCreator();

    public PlayerProfileSnapshot login(PlayerProfileRepository repository, UUID accountUuid,
                                       String accountName, PlayerProfileTemplate template, Instant now) {
        EchoPlayerId playerId = EchoPlayerId.fromAccountUuid(accountUuid);
        return repository.find(playerId)
                .map(existing -> withLogin(existing, loginIdentity(existing, template, accountName), now,
                        accountName))
                .map(repository::save)
                .orElseGet(() -> repository.save(creator.create(accountUuid, accountName, template, now)));
    }

    public PlayerProfileSnapshot logout(PlayerProfileRepository repository, PlayerProfileSnapshot snapshot,
                                        Instant now) {
        return repository.save(new PlayerProfileSnapshot(snapshot.playerId(), snapshot.identity(), snapshot.spark(),
                snapshot.appearance(), snapshot.templateId(), snapshot.templateFingerprint(),
                snapshot.lifecycle().loggedOut(now), snapshot.profileRevision().next(),
                snapshot.repositoryRevision()));
    }

    private PlayerIdentitySnapshot loginIdentity(
            PlayerProfileSnapshot snapshot,
            PlayerProfileTemplate template,
            String accountName
    ) {
        PlayerIdentitySnapshot identity = snapshot.identity().withAccountName(accountName);
        if (identity.nameMode() == PlayerNameMode.ACCOUNT && template.nameMode() == PlayerNameMode.CUSTOM) {
            return identity.withCustomName(template.customDisplayName().orElseThrow());
        }
        return identity;
    }

    private PlayerProfileSnapshot withLogin(PlayerProfileSnapshot snapshot,
                                            PlayerIdentitySnapshot identity,
                                            Instant now,
                                            String accountName) {
        return new PlayerProfileSnapshot(snapshot.playerId(), identity, snapshot.spark(), snapshot.appearance(),
                snapshot.templateId(), snapshot.templateFingerprint(), snapshot.lifecycle().loggedIn(now,
                accountName), snapshot.profileRevision().next(), snapshot.repositoryRevision());
    }
}
