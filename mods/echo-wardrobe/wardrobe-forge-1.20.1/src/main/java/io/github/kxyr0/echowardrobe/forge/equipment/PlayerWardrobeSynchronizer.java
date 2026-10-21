package io.github.kxyr0.echowardrobe.forge.equipment;

import io.github.kxyr0.echowardrobe.forge.ForgeWardrobeRuntimeState;
import java.util.Objects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Recomputes wardrobe appearance from Minecraft-owned equipment changes.
 */
public final class PlayerWardrobeSynchronizer {
    private final ForgeWardrobeRuntimeState state;
    private final ForgeEquipmentSnapshots snapshots = new ForgeEquipmentSnapshots();

    public PlayerWardrobeSynchronizer(ForgeWardrobeRuntimeState state) {
        this.state = Objects.requireNonNull(state, "state");
    }

    public void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            observe(player);
        }
    }

    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        observe(event.getEntity());
    }

    private void observe(Player player) {
        state.observe(player.getUUID(), snapshots.current(player));
    }
}
