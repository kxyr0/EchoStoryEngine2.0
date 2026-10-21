package io.github.kxyr0.echonpcbackpack.script;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackCommandId;
import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackCommandResult;
import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackEquipRequest;
import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackUnequipRequest;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPolicy;
import io.github.kxyr0.echonpcbackpack.api.world.BackpackBlockLocation;
import io.github.kxyr0.echonpcbackpack.api.world.DimensionId;
import io.github.kxyr0.echonpcbackpack.forge.api.NpcBackpackServiceBridge;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

/**
 * One canonical binding target shared by Story, Dialogue, Trigger and future EchoScript hosts.
 */
public final class NpcBackpackScriptFacade {
    private final MinecraftServer server;

    public NpcBackpackScriptFacade(MinecraftServer server) {
        this.server = java.util.Objects.requireNonNull(server, "server");
    }

    public NpcBackpackCommandResult equip(String npcId, String dimension, BlockPosition position,
            String commandId) {
        var service = service();
        return service.equip(new NpcBackpackEquipRequest(new ActorId(npcId), location(dimension, position),
                new NpcBackpackCommandId(commandId), NpcBackpackTransitionPolicy.IMMEDIATE));
    }

    public NpcBackpackCommandResult unequip(String npcId, String dimension, BlockPosition position,
            String commandId) {
        var service = service();
        return service.unequip(new NpcBackpackUnequipRequest(new ActorId(npcId), location(dimension, position),
                new NpcBackpackCommandId(commandId), NpcBackpackTransitionPolicy.IMMEDIATE));
    }

    public NpcBackpackCommandResult equip(String owner, Map<String, String> values) {
        NpcBackpackScriptPayload payload = NpcBackpackScriptPayload.read(values);
        return equip(payload.actorId(), payload.dimension(), payload.position(), payload.commandId(owner, "equip"));
    }

    public NpcBackpackCommandResult unequip(String owner, Map<String, String> values) {
        NpcBackpackScriptPayload payload = NpcBackpackScriptPayload.read(values);
        return unequip(payload.actorId(), payload.dimension(), payload.position(),
                payload.commandId(owner, "unequip"));
    }

    public Optional<NpcBackpackCommandResult> command(String commandId) {
        return service().command(new NpcBackpackCommandId(commandId));
    }

    public boolean isWearing(String npcId) {
        return service().isWearing(new ActorId(npcId));
    }

    public boolean hasTransition(String npcId) {
        return service().hasTransition(new ActorId(npcId));
    }

    public boolean backpackIsAt(String dimension, BlockPosition position) {
        return service().backpackIsAt(location(dimension, position));
    }

    private io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackService service() {
        return NpcBackpackServiceBridge.lookup(server).service()
                .orElseThrow(() -> new IllegalStateException("Echo NPC backpack service is unavailable."));
    }

    private BackpackBlockLocation location(String dimension, BlockPosition position) {
        return new BackpackBlockLocation(new DimensionId(dimension), position);
    }
}
