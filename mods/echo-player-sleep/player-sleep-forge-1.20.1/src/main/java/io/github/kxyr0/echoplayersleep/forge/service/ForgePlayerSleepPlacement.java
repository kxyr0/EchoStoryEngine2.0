package io.github.kxyr0.echoplayersleep.forge.service;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepDreamDestination;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import io.github.kxyr0.echoplayersleep.api.world.BlockPosition;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import io.github.kxyr0.echoplayersleep.forge.interception.StoryWakePermit;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import org.slf4j.Logger;

public final class ForgePlayerSleepPlacement {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Supplier<MinecraftServer> server;

    public ForgePlayerSleepPlacement(Supplier<MinecraftServer> server) {
        this.server = server;
    }

    public Optional<ServerPlayer> player(PlayerRef player) {
        MinecraftServer current = server.get();
        return current == null ? Optional.empty()
                : Optional.ofNullable(current.getPlayerList().getPlayer(player.uuid()));
    }

    public boolean enterDream(ServerPlayer player, PlayerSleepDreamDestination destination) {
        return releaseBedPose(player) && teleportToDream(player, destination);
    }

    public boolean canEnterDream(PlayerSleepDreamDestination destination) {
        return level(destination.dimension().value()).isPresent();
    }

    public boolean releaseBedPose(ServerPlayer player) {
        try (StoryWakePermit.Scope ignored = StoryWakePermit.open(player.getUUID())) {
            player.stopSleepInBed(true, true);
        }
        if (player.isSleeping()) {
            LOGGER.warn("Echo Player Sleep could not release vanilla bed pose player={} uuid={}.",
                    player.getGameProfile().getName(), player.getUUID());
            return false;
        }
        return true;
    }

    public boolean teleportToDream(ServerPlayer player, PlayerSleepDreamDestination destination) {
        Optional<ServerLevel> dream = level(destination.dimension().value());
        if (dream.isEmpty()) {
            return false;
        }
        LOGGER.info("Echo Player Sleep entering dream player={} dimension={} x={} y={} z={} yaw={} pitch={}.",
                player.getGameProfile().getName(), destination.dimension().value(),
                destination.position().x(), destination.position().y(), destination.position().z(),
                destination.yaw(), destination.pitch());
        player.teleportTo(dream.orElseThrow(), destination.position().x(), destination.position().y(),
                destination.position().z(), java.util.Set.<RelativeMovement>of(),
                destination.yaw(), destination.pitch());
        return true;
    }

    public boolean returnToBed(ServerPlayer player, PlayerSleepSessionSnapshot snapshot) {
        Optional<ServerLevel> origin = level(snapshot.bed().dimension().value());
        if (origin.isEmpty()) {
            return false;
        }
        BlockPosition wake = snapshot.bed().safeWakePositions().stream().findFirst()
                .orElse(snapshot.bed().canonicalPosition());
        LOGGER.info("Echo Player Sleep returning player={} dimension={} x={} y={} z={} yaw={} pitch={}.",
                player.getGameProfile().getName(), snapshot.bed().dimension().value(),
                wake.x() + 0.5D, wake.y(), wake.z() + 0.5D,
                snapshot.origin().yaw(), snapshot.origin().pitch());
        player.teleportTo(origin.orElseThrow(), wake.x() + 0.5D, wake.y(), wake.z() + 0.5D,
                java.util.Set.<RelativeMovement>of(), snapshot.origin().yaw(), snapshot.origin().pitch());
        return true;
    }

    void completeNight(PlayerSleepSessionSnapshot snapshot) {
        level(snapshot.origin().dimension().value())
                .ifPresent(level -> level.setDayTime(level.getDayTime() + 24000L - level.getDayTime() % 24000L));
    }

    private Optional<ServerLevel> level(String dimension) {
        MinecraftServer current = server.get();
        if (current == null) {
            return Optional.empty();
        }
        ResourceLocation id = new ResourceLocation(dimension);
        return Optional.ofNullable(current.getLevel(ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, id)));
    }
}
