package io.github.kxyr0.echoplayer.core.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echoplayer.api.appearance.PlayerBaseSkin;
import io.github.kxyr0.echoplayer.api.identity.PlayerNameMode;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileTemplateId;
import io.github.kxyr0.echoplayer.api.spark.PlayerSpark;
import io.github.kxyr0.echoplayer.core.repository.InMemoryPlayerProfileRepository;
import io.github.kxyr0.echoplayer.core.template.PlayerProfileTemplate;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PlayerLifecycleCheckpointTest {
    @Test
    void secondLoginPreservesProfileAndUpdatesAccountNameOnly() {
        PlayerLifecycleCheckpoint lifecycle = new PlayerLifecycleCheckpoint();
        InMemoryPlayerProfileRepository repository = new InMemoryPlayerProfileRepository();
        UUID accountUuid = UUID.fromString("11111111-1111-1111-1111-111111111111");

        var first = lifecycle.login(repository, accountUuid, "First", PlayerProfileTemplate.fallback(),
                Instant.parse("2026-07-25T00:00:00Z"));
        var second = lifecycle.login(repository, accountUuid, "Second", PlayerProfileTemplate.fallback(),
                Instant.parse("2026-07-25T01:00:00Z"));

        assertEquals(first.playerId(), second.playerId());
        assertEquals(1, repository.size());
        assertEquals("Second", second.identity().accountName());
        assertEquals("Лололошка", second.identity().resolvedDisplayName());
        assertEquals(first.templateFingerprint(), second.templateFingerprint());
    }

    @Test
    void loginMigratesOldAccountDefaultToCurrentCustomDefaultName() {
        PlayerLifecycleCheckpoint lifecycle = new PlayerLifecycleCheckpoint();
        InMemoryPlayerProfileRepository repository = new InMemoryPlayerProfileRepository();
        UUID accountUuid = UUID.fromString("22222222-2222-2222-2222-222222222222");

        lifecycle.login(repository, accountUuid, "OldAccount", accountTemplate(),
                Instant.parse("2026-07-25T00:00:00Z"));
        var migrated = lifecycle.login(repository, accountUuid, "NewAccount", PlayerProfileTemplate.fallback(),
                Instant.parse("2026-07-25T01:00:00Z"));

        assertEquals(PlayerNameMode.CUSTOM, migrated.identity().nameMode());
        assertEquals("NewAccount", migrated.identity().accountName());
        assertEquals("Лололошка", migrated.identity().resolvedDisplayName());
    }

    private static PlayerProfileTemplate accountTemplate() {
        return new PlayerProfileTemplate(PlayerProfileTemplateId.parse("jdh:player/default"),
                PlayerNameMode.ACCOUNT, Optional.empty(), PlayerSpark.full(100),
                PlayerBaseSkin.snapshot(0L), "old-account-default");
    }
}
