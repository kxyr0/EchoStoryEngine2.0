package io.github.kxyr0.echostoryengine.story.definition.environment.model;

import io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation;

/**
 * One authored change inside an environment state.
 */
public sealed interface EnvironmentChangeDef permits SetBlockChangeDef, ClearBlockChangeDef {
    EnvironmentPosition position();

    SourceLocation location();
}
