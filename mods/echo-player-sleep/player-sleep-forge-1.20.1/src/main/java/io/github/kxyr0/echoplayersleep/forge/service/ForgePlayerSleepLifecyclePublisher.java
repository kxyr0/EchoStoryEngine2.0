package io.github.kxyr0.echoplayersleep.forge.service;

import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepEventKind;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import io.github.kxyr0.echoplayersleep.forge.api.lifecycle.PlayerSleepLifecyclePhase;
import io.github.kxyr0.echoplayersleep.forge.event.PlayerSleepEventPublisher;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;

public final class ForgePlayerSleepLifecyclePublisher {
    private final Supplier<MinecraftServer> server;
    private final PlayerSleepEventPublisher eventPublisher;

    public ForgePlayerSleepLifecyclePublisher(
            Supplier<MinecraftServer> server,
            PlayerSleepEventPublisher eventPublisher) {
        this.server = Objects.requireNonNull(server, "server");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
    }

    public void publish(PlayerSleepLifecyclePhase phase, PlayerSleepSessionSnapshot snapshot) {
        switch (phase) {
            case DREAM_ENTERED -> eventPublisher.publish(PlayerSleepEventKind.ENTERED_DREAM, phase, snapshot, tick());
            case RETURNED -> eventPublisher.publish(PlayerSleepEventKind.RETURNED, phase, snapshot, tick());
            case COMPLETED -> eventPublisher.publish(PlayerSleepEventKind.COMPLETED, phase, snapshot, tick());
            case FAILED -> eventPublisher.publish(PlayerSleepEventKind.FAILED, phase, snapshot, tick());
            default -> eventPublisher.publishLifecycle(phase, snapshot);
        }
    }

    private long tick() {
        MinecraftServer current = server.get();
        return current == null ? 0L : current.getTickCount();
    }
}
