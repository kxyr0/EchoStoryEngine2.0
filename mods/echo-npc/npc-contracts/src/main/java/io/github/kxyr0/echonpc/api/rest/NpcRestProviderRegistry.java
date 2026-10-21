package io.github.kxyr0.echonpc.api.rest;

import io.github.kxyr0.echonpc.api.block.BlockId;
import java.util.List;
import java.util.Optional;

/**
 * Read-only public view of available rest providers.
 */
public interface NpcRestProviderRegistry {
    List<NpcRestProvider> providers();

    Optional<NpcRestProvider> provider(BlockId blockId);
}
