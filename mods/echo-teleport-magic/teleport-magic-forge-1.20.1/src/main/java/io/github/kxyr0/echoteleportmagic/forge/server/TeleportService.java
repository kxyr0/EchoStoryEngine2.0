package io.github.kxyr0.echoteleportmagic.forge.server;

import io.github.kxyr0.echoteleportmagic.api.TeleportDestination;
import io.github.kxyr0.echoteleportmagic.api.TeleportExecution;
import io.github.kxyr0.echoteleportmagic.api.TeleportExecutionId;
import io.github.kxyr0.echoteleportmagic.api.TeleportState;
import io.github.kxyr0.echoteleportmagic.forge.persistence.TeleportReceiptStore;
import io.github.kxyr0.echoteleportmagic.runtime.TeleportCoordinator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.ModList;

public final class TeleportService {
    private final MinecraftServer server;
    private final TeleportReceiptStore receipts;
    private final TeleportCoordinator coordinator;
    private final Map<TeleportExecutionId, Consumer<TeleportExecution>> completions = new HashMap<>();

    public TeleportService(MinecraftServer server) {
        this.server = server;
        receipts = new TeleportReceiptStore(server.getWorldPath(LevelResource.ROOT).resolve("echo_teleport_magic/receipts"));
        Consumer<TeleportExecution> events = ModList.get().isLoaded("echo_events")
                ? new io.github.kxyr0.echoteleportmagic.forge.integration.TeleportEvents(server) : ignored -> { };
        coordinator = new TeleportCoordinator(new ForgeTeleportPort(server, receipts, this::finished, events));
    }
    public TeleportExecution start(LivingEntity subject, TeleportDestination destination,
            TeleportExecutionId id, String owner, String provenance, boolean preview,
            Consumer<TeleportExecution> complete) {
        return startOwned(subject, destination, id, owner, owner, provenance, preview, complete);
    }
    public TeleportExecution startOwned(LivingEntity subject, TeleportDestination destination,
            TeleportExecutionId id, String owner, String rootOwner, String provenance, boolean preview,
            Consumer<TeleportExecution> complete) {
        return startActor(subject, destination, id, owner, rootOwner, subject.getUUID().toString(), provenance, preview, complete);
    }
    public TeleportExecution startActor(LivingEntity subject, TeleportDestination destination,
            TeleportExecutionId id, String owner, String rootOwner, String subjectReference, String provenance, boolean preview,
            Consumer<TeleportExecution> complete) {
        if (!server.isSameThread()) throw new IllegalStateException("Teleport requires server thread");
        var request = new TeleportExecution(id, subject.getUUID(), TeleportSubjects.anchor(subject), destination,
                server.overworld().getGameTime() + 4, id.value().getLeastSignificantBits(), TeleportState.CREATED,
                false, preview, owner, provenance, 1, rootOwner, subjectReference);
        if (!preview) receipts.validate(request);
        var result = coordinator.start(request);
        if (!result.state().terminal()) completions.putIfAbsent(id, complete);
        return result;
    }
    private void finished(TeleportExecution e) {
        var completion = completions.remove(e.id());
        if (completion != null) completion.accept(e);
    }
    public void tick() { coordinator.tick(server.overworld().getGameTime()); }
    public TeleportCoordinator coordinator() { return coordinator; }
    public void stop() { for (var e : coordinator.active()) coordinator.cancel(e.id()); }
}
