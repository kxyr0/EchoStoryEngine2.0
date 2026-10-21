package io.github.kxyr0.echonpc.api.rest;

import io.github.kxyr0.echonpc.api.block.BlockId;

/**
 * Public rest provider descriptor. Platform adapters perform the concrete attachment.
 */
public interface NpcRestProvider {
    String providerId();

    boolean supports(BlockId blockId);
}
