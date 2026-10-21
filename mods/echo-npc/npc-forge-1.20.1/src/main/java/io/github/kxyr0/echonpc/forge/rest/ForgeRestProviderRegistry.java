package io.github.kxyr0.echonpc.forge.rest;

import io.github.kxyr0.echonpc.api.block.BlockId;
import io.github.kxyr0.echonpc.api.rest.NpcRestProvider;
import io.github.kxyr0.echonpc.api.rest.NpcRestProviderRegistry;
import io.github.kxyr0.echonpc.api.rest.command.NpcSleepRequest;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.core.rest.NpcRestAttachment;
import io.github.kxyr0.echonpc.forge.entity.EchoNpcEntity;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;

/**
 * Forge provider registry for rest attachments.
 */
public final class ForgeRestProviderRegistry implements NpcRestProviderRegistry {
    private final VanillaBedRestProvider vanillaBed = new VanillaBedRestProvider();

    @Override
    public List<NpcRestProvider> providers() {
        return List.of(vanillaBed);
    }

    @Override
    public Optional<NpcRestProvider> provider(BlockId blockId) {
        return providers().stream().filter(provider -> provider.supports(blockId)).findFirst();
    }

    public ForgeRestResolution resolve(ServerLevel level, NpcSleepRequest request, String commandId, long revision) {
        if (provider(request.expectedBlock()).isEmpty()) {
            return ForgeRestResolution.rejected(NpcResults.SleepStatus.PROVIDER_NOT_FOUND,
                    Optional.empty(), "NPC_SLEEP_PROVIDER_NOT_FOUND",
                    "No rest provider supports expectedBlock.");
        }
        return vanillaBed.resolve(level, request, commandId, revision);
    }

    public Optional<String> validateAttachment(ServerLevel level, NpcRestAttachment attachment) {
        if (VanillaBedRestProvider.ID.equals(attachment.providerId())) {
            return vanillaBed.validateAttachment(level, attachment);
        }
        return Optional.of("NPC_SLEEP_PROVIDER_NOT_FOUND");
    }

    public void applySleep(ServerLevel level, EchoNpcEntity entity, NpcRestAttachment attachment) {
        if (VanillaBedRestProvider.ID.equals(attachment.providerId())) {
            vanillaBed.applySleep(level, entity, attachment);
        }
    }

    public void release(ServerLevel level, EchoNpcEntity entity, NpcRestAttachment attachment) {
        if (VanillaBedRestProvider.ID.equals(attachment.providerId())) {
            vanillaBed.release(level, entity, attachment);
        }
    }

    public io.github.kxyr0.echonpc.api.spatial.NpcPosition wakePosition(NpcRestAttachment attachment) {
        return vanillaBed.wakePosition(attachment);
    }
}
