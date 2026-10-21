package io.github.kxyr0.echoplayer.api.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class EchoPlayerIdTest {
    @Test
    void sameUuidGivesSamePlayerId() {
        UUID uuid = UUID.fromString("11111111-2222-3333-4444-555555555555");

        assertEquals(EchoPlayerId.fromAccountUuid(uuid), EchoPlayerId.fromAccountUuid(uuid));
    }

    @Test
    void usernameChangeDoesNotChangePlayerId() {
        UUID uuid = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        PlayerIdentitySnapshot before = PlayerIdentitySnapshot.account(EchoPlayerId.fromAccountUuid(uuid), "First");

        PlayerIdentitySnapshot after = before.withAccountName("Second");

        assertEquals(before.playerId(), after.playerId());
        assertEquals("Second", after.resolvedDisplayName());
    }

    @Test
    void invalidUuidFails() {
        assertThrows(IllegalArgumentException.class, () -> EchoPlayerId.parse("not-a-uuid"));
    }
}
