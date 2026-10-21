package io.github.kxyr0.echostoryengine.story.extension.forge;

/**
 * Bounded registration surface exposed only during the registration event.
 */
public interface ForgeStoryExtensionRegistrar {
    boolean register(ForgeStoryExtensionFactory factory);
}
