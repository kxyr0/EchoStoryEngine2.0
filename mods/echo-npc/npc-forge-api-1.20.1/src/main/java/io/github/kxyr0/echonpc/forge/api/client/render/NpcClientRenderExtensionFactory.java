package io.github.kxyr0.echonpc.forge.api.client.render;

/**
 * Creates one renderer-bound extension instance.
 */
@FunctionalInterface
public interface NpcClientRenderExtensionFactory {
    NpcClientRenderExtension create();
}
