package io.github.kxyr0.echonpc.forge.rest;

import io.github.kxyr0.echonpc.api.block.BlockId;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.core.rest.NpcRestAttachment;
import java.util.Objects;
import java.util.Optional;

/**
 * Forge-side exact-bed resolution result.
 */
public record ForgeRestResolution(
        NpcResults.SleepStatus status,
        Optional<NpcRestAttachment> attachment,
        Optional<BlockId> actualBlock,
        String diagnosticCode,
        String message
) {
    public ForgeRestResolution {
        status = Objects.requireNonNull(status, "status");
        attachment = Objects.requireNonNull(attachment, "attachment");
        actualBlock = Objects.requireNonNull(actualBlock, "actualBlock");
        diagnosticCode = Objects.requireNonNull(diagnosticCode, "diagnosticCode");
        message = Objects.requireNonNull(message, "message");
    }

    static ForgeRestResolution accepted(NpcRestAttachment attachment, BlockId actualBlock) {
        return new ForgeRestResolution(NpcResults.SleepStatus.SLEEPING, Optional.of(attachment),
                Optional.of(actualBlock), "NPC_SLEEP_ATTACHED", "NPC attached to exact bed.");
    }

    static ForgeRestResolution rejected(
            NpcResults.SleepStatus status,
            Optional<BlockId> actualBlock,
            String diagnosticCode,
            String message
    ) {
        return new ForgeRestResolution(status, Optional.empty(), actualBlock, diagnosticCode, message);
    }
}
