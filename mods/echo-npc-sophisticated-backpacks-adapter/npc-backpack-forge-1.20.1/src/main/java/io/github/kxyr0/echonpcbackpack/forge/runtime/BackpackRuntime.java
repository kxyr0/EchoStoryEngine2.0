package io.github.kxyr0.echonpcbackpack.forge.runtime;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackCommandId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackMarkerId;
import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackCommandResult;
import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackEquipRequest;
import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackReservationView;
import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackService;
import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackUnequipRequest;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackOutcome;
import io.github.kxyr0.echonpcbackpack.api.view.NpcBackpackDiagnostic;
import io.github.kxyr0.echonpcbackpack.api.view.NpcBackpackView;
import io.github.kxyr0.echonpcbackpack.api.world.BackpackBlockLocation;
import io.github.kxyr0.echonpcbackpack.forge.network.BackpackNetwork;
import io.github.kxyr0.echonpcbackpack.forge.marker.BackpackMarkerCoordinator;
import io.github.kxyr0.echonpcbackpack.forge.state.BackpackActorRecord;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class BackpackRuntime implements NpcBackpackService {
    private final BackpackRuntimeState state = new BackpackRuntimeState();
    private final BackpackCommandCoordinator commands = new BackpackCommandCoordinator(state);
    private final BackpackTransitionTicker ticker = new BackpackTransitionTicker(state);
    private final BackpackMarkerCoordinator markers = new BackpackMarkerCoordinator(state);
    private final BackpackRecoveryCoordinator recovery = new BackpackRecoveryCoordinator(state);

    public void start(MinecraftServer server) throws IOException {
        state.start(server);
        recovery.recoverAll();
    }

    public void stop() {
        state.stop();
    }

    public void tick() {
        if (state.ready()) ticker.tick();
    }

    public void sendFullSnapshots(ServerPlayer player) {
        if (state.ready()) state.actors.values().forEach(record -> BackpackNetwork.send(player, record));
    }

    @Override
    public NpcBackpackCommandResult equip(NpcBackpackEquipRequest request) {
        return ready(request.commandId()) ? commands.equip(request) : unavailable(request.commandId());
    }

    @Override
    public NpcBackpackCommandResult unequip(NpcBackpackUnequipRequest request) {
        return ready(request.commandId()) ? commands.unequip(request) : unavailable(request.commandId());
    }

    @Override
    public NpcBackpackCommandResult marker(ActorId actorId, NpcBackpackMarkerId markerId, String occurrenceId) {
        NpcBackpackCommandId fallback = new NpcBackpackCommandId("echo_npc_sophisticated_backpacks:marker_"
                + Integer.toUnsignedString((actorId.value() + markerId.value()).hashCode(), 36));
        return ready(fallback) ? markers.accept(actorId, markerId, occurrenceId) : unavailable(fallback);
    }

    @Override
    public NpcBackpackCommandResult recover(ActorId actorId, NpcBackpackCommandId commandId) {
        return ready(commandId) ? recovery.retry(actorId, commandId) : unavailable(commandId);
    }

    @Override
    public Optional<NpcBackpackView> inspect(ActorId actorId) {
        BackpackActorRecord record = state.actors.get(actorId.value());
        return record == null ? Optional.empty() : Optional.of(state.views.view(record));
    }

    @Override
    public Optional<NpcBackpackCommandResult> command(NpcBackpackCommandId commandId) {
        BackpackActorRecord active = state.actors.values().stream()
                .filter(record -> record.commandId.filter(commandId::equals).isPresent()).findFirst().orElse(null);
        if (active != null) return Optional.of(new NpcBackpackCommandResult(NpcBackpackOutcome.ACCEPTED,
                commandId, Optional.of(state.views.view(active)), "ENPC-SB-ACTIVE", "Command is active."));
        String outcome = state.terminalCommands.get(commandId.value());
        return outcome == null ? Optional.empty() : Optional.of(new NpcBackpackCommandResult(
                NpcBackpackOutcome.valueOf(outcome), commandId, Optional.empty(),
                "ENPC-SB-TERMINAL", "Command is terminal."));
    }

    @Override
    public boolean isWearing(ActorId actorId) {
        BackpackActorRecord record = state.actors.get(actorId.value());
        return record != null && record.carryingPayload()
                && record.attachmentPoint == io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackAttachmentPoint.BACK;
    }

    @Override
    public boolean hasTransition(ActorId actorId) {
        BackpackActorRecord record = state.actors.get(actorId.value());
        return record != null && record.transitioning();
    }

    @Override
    public boolean backpackIsAt(BackpackBlockLocation location) {
        return state.level(location).flatMap(level -> state.access.inspect(level, block(location))).isPresent();
    }

    @Override
    public List<NpcBackpackReservationView> reservations() {
        return state.actors.values().stream().filter(BackpackActorRecord::transitioning)
                .filter(record -> record.reservationId.isPresent()).map(record -> new NpcBackpackReservationView(
                        record.reservationId.orElseThrow(), record.actorId,
                        record.source.or(() -> record.destination).orElseThrow(), state.gameTick())).toList();
    }

    @Override
    public List<NpcBackpackDiagnostic> diagnostics() {
        return state.diagnostics();
    }

    private boolean ready(NpcBackpackCommandId ignored) {
        return state.ready() && state.server().isSameThread();
    }

    private NpcBackpackCommandResult unavailable(NpcBackpackCommandId commandId) {
        return new NpcBackpackCommandResult(NpcBackpackOutcome.REJECTED, commandId, Optional.empty(),
                "ENPC-SB-SERVICE-UNAVAILABLE", "Backpack service is unavailable or called off server thread.");
    }

    private BlockPos block(BackpackBlockLocation location) {
        return new BlockPos(location.position().x(), location.position().y(), location.position().z());
    }
}
