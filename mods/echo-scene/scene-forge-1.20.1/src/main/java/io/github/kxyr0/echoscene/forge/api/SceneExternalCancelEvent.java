package io.github.kxyr0.echoscene.forge.api;

import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import net.minecraftforge.eventbus.api.Event;

public final class SceneExternalCancelEvent extends Event {
    private final SceneExecutionId owner;
    public SceneExternalCancelEvent(SceneExecutionId owner) { this.owner = owner; }
    public SceneExecutionId owner() { return owner; }
}
