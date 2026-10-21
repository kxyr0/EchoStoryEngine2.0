package io.github.kxyr0.echostoryengine.story.api;

/**
 * Public entrypoint for advancing the story runtime.
 *
 * <p>The API accepts only neutral Story Engine contracts. It is stable across platform adapters,
 * does not expose implementation classes, and never accepts or returns Minecraft or Forge types.
 * Calls are expected to happen on the authoritative server execution context chosen by the platform adapter.
 */
@FunctionalInterface
public interface StoryEngineApi {
    /**
     * Advances one execution with one neutral runtime input.
     *
     * @param input neutral story input; never {@code null}
     * @return immutable transition result with the new execution state and diagnostics
     * @throws NullPointerException when input is {@code null}
     * @throws IllegalArgumentException when the input violates public contract invariants
     */
    StoryRuntimeResult advance(StoryRuntimeInput input);
}
