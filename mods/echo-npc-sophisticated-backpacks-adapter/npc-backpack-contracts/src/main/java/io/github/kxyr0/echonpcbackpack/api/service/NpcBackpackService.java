package io.github.kxyr0.echonpcbackpack.api.service;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackCommandId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackMarkerId;
import io.github.kxyr0.echonpcbackpack.api.view.NpcBackpackDiagnostic;
import io.github.kxyr0.echonpcbackpack.api.view.NpcBackpackView;
import io.github.kxyr0.echonpcbackpack.api.world.BackpackBlockLocation;
import java.util.List;
import java.util.Optional;

public interface NpcBackpackService {
    NpcBackpackCommandResult equip(NpcBackpackEquipRequest request);

    NpcBackpackCommandResult unequip(NpcBackpackUnequipRequest request);

    NpcBackpackCommandResult marker(ActorId actorId, NpcBackpackMarkerId markerId, String occurrenceId);

    NpcBackpackCommandResult recover(ActorId actorId, NpcBackpackCommandId commandId);

    Optional<NpcBackpackView> inspect(ActorId actorId);

    Optional<NpcBackpackCommandResult> command(NpcBackpackCommandId commandId);

    boolean isWearing(ActorId actorId);

    boolean hasTransition(ActorId actorId);

    boolean backpackIsAt(BackpackBlockLocation location);

    List<NpcBackpackReservationView> reservations();

    List<NpcBackpackDiagnostic> diagnostics();
}
