package io.github.kxyr0.echonpc.forge.api.client.render;

/**
 * Public client render extension called at the NPC head render boundary.
 */
@FunctionalInterface
public interface NpcClientRenderExtension {
    NpcClientRenderResult render(NpcHeadRenderContext context);
}
