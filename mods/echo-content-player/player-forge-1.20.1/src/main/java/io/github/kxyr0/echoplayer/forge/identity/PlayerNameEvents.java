package io.github.kxyr0.echoplayer.forge.identity;

import io.github.kxyr0.echoplayer.forge.ForgePlayerRuntimeState;
import io.github.kxyr0.echoplayer.forge.client.PlayerClientMirror;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Applies persisted Echo display names to vanilla display name surfaces.
 */
public final class PlayerNameEvents {
    private final ForgePlayerRuntimeState state;

    public PlayerNameEvents(ForgePlayerRuntimeState state) {
        this.state = Objects.requireNonNull(state, "state");
    }

    public void onNameFormat(PlayerEvent.NameFormat event) {
        resolvedName(event.getEntity()).ifPresent(name -> event.setDisplayname(Component.literal(name)));
    }

    public void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        resolvedName(event.getEntity()).ifPresent(name -> event.setDisplayName(Component.literal(name)));
    }

    private Optional<String> resolvedName(Player player) {
        if (player.level().isClientSide()) {
            return PlayerClientMirror.INSTANCE.resolvedName(player.getUUID());
        }
        return state.services().findByAccountUuid(player.getUUID())
                .map(snapshot -> snapshot.identity().resolvedDisplayName());
    }
}
