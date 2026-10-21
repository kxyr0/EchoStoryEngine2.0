package io.github.kxyr0.echoseating.forge.runtime;

import io.github.kxyr0.echoseating.api.world.PlayerRef;
import io.github.kxyr0.echoseating.core.lifecycle.PlayerRidingTransition;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class PlayerRidingObserver {
    private final ForgeSeatingServices services;
    private final ForgeSeatAnchorResolver resolver = new ForgeSeatAnchorResolver();
    private final List<Pending> pending = new ArrayList<>();
    private long generation;

    public PlayerRidingObserver(ForgeSeatingServices services) {
        this.services = services;
    }

    @SubscribeEvent
    public void onEntityMount(EntityMountEvent event) {
        if (!(event.getEntityMounting() instanceof ServerPlayer player)) {
            return;
        }
        if (event.isMounting()) {
            pending.add(Pending.mount(player.getUUID(), event.getEntityBeingMounted(), ++generation));
        } else if (event.isDismounting()) {
            pending.add(Pending.dismount(player.getUUID(), ++generation));
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        List<Pending> ready = List.copyOf(pending);
        pending.clear();
        ready.forEach(value -> confirm(server, value));
    }

    private void confirm(MinecraftServer server, Pending value) {
        ServerPlayer player = server.getPlayerList().getPlayer(value.player());
        if (player == null) {
            return;
        }
        Entity vehicle = player.getVehicle();
        if (vehicle == null) {
            services.apply(new PlayerRidingTransition(new PlayerRef(player.getUUID()),
                    value.previousVehicle(), Optional.empty(), value.generation(), server.getTickCount()));
            return;
        }
        if (value.vehicle().isPresent() && !vehicle.getUUID().equals(value.vehicle().orElseThrow())) {
            return;
        }
        services.apply(new PlayerRidingTransition(new PlayerRef(player.getUUID()),
                value.previousVehicle(), Optional.of(resolver.resolve(player, vehicle,
                server.getTickCount(), services.catalogRevision())), value.generation(), server.getTickCount()));
    }

    private record Pending(UUID player, Optional<UUID> previousVehicle,
            Optional<UUID> vehicle, long generation) {
        static Pending mount(UUID player, Entity vehicle, long generation) {
            return new Pending(player, Optional.empty(), Optional.of(vehicle.getUUID()), generation);
        }

        static Pending dismount(UUID player, long generation) {
            return new Pending(player, Optional.empty(), Optional.empty(), generation);
        }
    }
}
