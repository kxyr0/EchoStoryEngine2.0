package io.github.kxyr0.echoplayersleep.forge.interception;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepBedSnapshot;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepOriginSnapshot;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import io.github.kxyr0.echoplayersleep.api.world.Vector3;
import io.github.kxyr0.echoplayersleep.forge.api.bed.PlayerSleepBedProvider;
import io.github.kxyr0.echoplayersleep.forge.bed.ForgeSleepIds;
import io.github.kxyr0.echoplayersleep.forge.service.ForgePlayerSleepServices;
import io.github.kxyr0.echoplayersleep.core.session.VanillaSleepConfirmation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.SleepingLocationCheckEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

public final class VanillaSleepInterception {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long CONFIRMATION_TIMEOUT_TICKS = 40L;
    private final ForgePlayerSleepServices services;
    private final Map<UUID, PendingSleepAttempt> pending = new LinkedHashMap<>();

    public VanillaSleepInterception(ForgePlayerSleepServices services) {
        this.services = services;
    }

    @SubscribeEvent
    public void onSleepAttempt(PlayerSleepInBedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getOptionalPos().isEmpty()) {
            return;
        }
        PlayerRef ref = new PlayerRef(player.getUUID());
        BlockPos bedPos = event.getOptionalPos().orElseThrow();
        boolean narrativeCandidate = services.activeArm(ref).isPresent();
        pending.put(player.getUUID(), new PendingSleepAttempt(ref, bedPos,
                player.serverLevel().getGameTime(), narrativeCandidate));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !services.owns(event.getServer())) {
            return;
        }
        pending.values().removeIf(attempt -> confirmOrExpire(event, attempt));
        enforceStoryBedPose();
    }

    @SubscribeEvent
    public void onSleepFinishedTime(SleepFinishedTimeEvent event) {
        if (!services.query().activeSessions().isEmpty()) {
            event.setTimeAddition(0L);
        }
    }

    @SubscribeEvent
    public void onSleepingTimeCheck(SleepingTimeCheckEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && StorySleepRegistry.ownsBedPose(player)) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public void onSleepingLocationCheck(SleepingLocationCheckEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && StorySleepRegistry.ownsBedPose(player)) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    private boolean confirmOrExpire(TickEvent.ServerTickEvent event, PendingSleepAttempt attempt) {
        ServerPlayer player = event.getServer().getPlayerList().getPlayer(attempt.player().uuid());
        if (player == null) {
            return true;
        }
        Optional<PlayerSleepBedSnapshot> bed = confirmedBed(player, attempt.bedPos());
        if (bed.isPresent()) {
            services.confirmVanillaSleep(confirmation(player, bed.orElseThrow(), event.getServer().getTickCount()));
            return true;
        }
        long age = player.serverLevel().getGameTime() - attempt.startedTick();
        if (age > CONFIRMATION_TIMEOUT_TICKS) {
            if (attempt.narrativeCandidate()) {
                services.vanillaConfirmationTimedOut(attempt.player());
            }
            return true;
        }
        return false;
    }

    private Optional<PlayerSleepBedSnapshot> confirmedBed(ServerPlayer player, BlockPos bedPos) {
        Optional<PlayerSleepBedProvider> provider = services.bedProviders().providerFor(player.level(), bedPos);
        return provider.flatMap(candidate -> candidate.snapshot(player, bedPos)
                .filter(snapshot -> candidate.confirmsSleeping(player, snapshot)));
    }

    private void enforceStoryBedPose() {
        services.query().activeSessions().stream()
                .filter(PlayerSleepSessionSnapshot::ownsBedPose)
                .forEach(this::enforceStoryBedPose);
    }

    private void enforceStoryBedPose(PlayerSleepSessionSnapshot snapshot) {
        services.player(snapshot.player()).ifPresent(player -> {
            BlockPos bedPos = ForgeSleepIds.blockPos(snapshot.bed().canonicalPosition());
            boolean alreadySleepingThere = player.isSleeping()
                    && player.getSleepingPos().filter(bedPos::equals).isPresent();
            if (alreadySleepingThere) {
                return;
            }
            LOGGER.warn("Echo Player Sleep restored story bed pose player={} uuid={} status={} bed={} "
                            + "wasSleeping={} sleepingPos={}.",
                    player.getGameProfile().getName(), player.getUUID(), snapshot.status(), bedPos,
                    player.isSleeping(), player.getSleepingPos().map(BlockPos::toString).orElse("empty"));
            player.startSleeping(bedPos);
            player.serverLevel().updateSleepingPlayerList();
        });
    }

    private VanillaSleepConfirmation confirmation(
            ServerPlayer player,
            PlayerSleepBedSnapshot bed,
            long serverTick) {
        BlockPos sleeping = player.getSleepingPos().orElse(ForgeSleepIds.blockPos(bed.canonicalPosition()));
        PlayerSleepOriginSnapshot origin = new PlayerSleepOriginSnapshot(ForgeSleepIds.dimension(player.level()),
                new Vector3(player.getX(), player.getY(), player.getZ()), player.getYRot(), player.getXRot(),
                ForgeSleepIds.position(sleeping), player.getInventory().selected, "");
        return new VanillaSleepConfirmation(new PlayerRef(player.getUUID()), bed, origin, serverTick);
    }
}
