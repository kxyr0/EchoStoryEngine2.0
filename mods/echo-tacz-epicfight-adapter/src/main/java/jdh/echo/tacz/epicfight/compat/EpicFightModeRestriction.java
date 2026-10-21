package jdh.echo.tacz.epicfight.compat;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import jdh.echo.tacz.epicfight.EchoTaczEpicFight;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.api.forgeevent.BattleModeSustainableEvent;
import yesman.epicfight.api.forgeevent.ChangePlayerModeEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@Mod.EventBusSubscriber(modid = EchoTaczEpicFight.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EpicFightModeRestriction {
    private static final Set<UUID> CLIENT_RESTRICTED_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> SERVER_RESTRICTED_PLAYERS = ConcurrentHashMap.newKeySet();

    private EpicFightModeRestriction() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBattleModeSustainable(BattleModeSustainableEvent event) {
        PlayerPatch<?> playerPatch = event.getPlayerPatch();
        if (playerPatch == null) {
            return;
        }

        Player player = playerPatch.getOriginal();
        if (player != null && TaczGunDetector.isGunHeld(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onChangePlayerMode(ChangePlayerModeEvent event) {
        PlayerPatch<?> playerPatch = event.getPlayerPatch();
        if (playerPatch == null || event.getPlayerMode() != PlayerPatch.PlayerMode.EPICFIGHT) {
            return;
        }

        Player player = playerPatch.getOriginal();
        if (player != null && TaczGunDetector.isGunHeld(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
        PlayerPatch<?> playerPatch = EpicFightCapabilities.getPlayerPatch(player);
        if (playerPatch == null) {
            return;
        }

        updateModeRestriction(player, playerPatch);
    }

    private static void updateModeRestriction(Player player, PlayerPatch<?> playerPatch) {
        Set<UUID> restrictedPlayers = restrictedPlayers(player);
        UUID playerId = player.getUUID();

        if (TaczGunDetector.isGunHeld(player)) {
            if (playerPatch.isEpicFightMode()) {
                restrictedPlayers.add(playerId);
                playerPatch.toVanillaMode(false);
            }
            return;
        }

        if (restrictedPlayers.remove(playerId) && playerPatch.isVanillaMode()) {
            playerPatch.toEpicFightMode(false);
        }
    }

    private static Set<UUID> restrictedPlayers(Player player) {
        return player.level().isClientSide ? CLIENT_RESTRICTED_PLAYERS : SERVER_RESTRICTED_PLAYERS;
    }
}
