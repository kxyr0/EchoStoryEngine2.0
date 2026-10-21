package io.github.kxyr0.echonpc.forge.rest;

import io.github.kxyr0.echonpc.api.rest.command.NpcSleepRequest;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.core.actor.ActorDesiredState;
import io.github.kxyr0.echonpc.core.platform.PlatformActorHandle;
import io.github.kxyr0.echonpc.core.rest.NpcRestAttachment;
import io.github.kxyr0.echonpc.forge.host.ForgeActorMaterializer;
import io.github.kxyr0.echonpc.forge.host.ForgeWorldIds;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Applies durable rest attachments at the Forge boundary.
 */
public final class ForgeRestRuntime {
    private final MinecraftServer server;
    private final ForgeActorMaterializer materializer;
    private final ForgeRestProviderRegistry providers = new ForgeRestProviderRegistry();

    public ForgeRestRuntime(MinecraftServer server, ForgeActorMaterializer materializer) {
        this.server = java.util.Objects.requireNonNull(server, "server");
        this.materializer = java.util.Objects.requireNonNull(materializer, "materializer");
    }

    public ForgeRestProviderRegistry providers() {
        return providers;
    }

    public ForgeRestResolution resolve(NpcSleepRequest request, String commandId, long revision) {
        ServerLevel level = server.getLevel(ForgeWorldIds.toKey(request.worldId()));
        if (level == null) {
            return ForgeRestResolution.rejected(NpcResults.SleepStatus.BED_NOT_FOUND, Optional.empty(),
                    "NPC_SLEEP_BED_NOT_FOUND", "Sleep dimension is missing.");
        }
        return providers.resolve(level, request, commandId, revision);
    }

    public Optional<String> apply(ActorDesiredState desired, Optional<PlatformActorHandle> handle) {
        if (desired.restAttachment().isEmpty()) {
            return Optional.empty();
        }
        ServerLevel level = server.getLevel(ForgeWorldIds.toKey(desired.worldId()));
        if (level == null || handle.isEmpty()) {
            return Optional.of("NPC_SLEEP_ATTACHMENT_REJECTED: entity or level missing.");
        }
        Optional<String> invalid = providers.validateAttachment(level, desired.restAttachment().orElseThrow());
        if (invalid.isPresent()) {
            return invalid;
        }
        var entity = materializer.findEntity(handle.orElseThrow());
        if (entity.isEmpty()) {
            return Optional.of("NPC_SLEEP_ATTACHMENT_REJECTED: entity missing.");
        }
        providers.applySleep(level, entity.orElseThrow(), desired.restAttachment().orElseThrow());
        return Optional.empty();
    }

    public void release(NpcRestAttachment attachment, Optional<PlatformActorHandle> handle) {
        ServerLevel level = server.getLevel(ForgeWorldIds.toKey(attachment.worldId()));
        if (level == null || handle.isEmpty()) {
            return;
        }
        materializer.findEntity(handle.orElseThrow())
                .ifPresent(entity -> providers.release(level, entity, attachment));
    }

    public NpcPosition wakePosition(NpcRestAttachment attachment) {
        return providers.wakePosition(attachment);
    }
}
