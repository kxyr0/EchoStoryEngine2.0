package io.github.kxyr0.echostoryengine.story.forge.extension.readmodel;

/**
 * Bounded read model line for `/echo_story extensions`.
 */
public record ExtensionSummary(
        String extensionId,
        String displayName,
        String implementationVersion,
        String requiredApiVersion,
        String status,
        int actionCount,
        int warningOrErrorCount) {
}
