package io.github.kxyr0.echomagic.forge.api.event;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/** Lets a void weapon sever links maintained by any independently loaded magic mod. */
public final class MagicLinkCutEvent extends Event {
    private final ServerPlayer caster;
    private final LivingEntity target;
    private boolean handled;

    public MagicLinkCutEvent(ServerPlayer caster, LivingEntity target) {
        this.caster = Objects.requireNonNull(caster, "caster");
        this.target = Objects.requireNonNull(target, "target");
    }

    public ServerPlayer caster() {
        return caster;
    }

    public LivingEntity target() {
        return target;
    }

    public boolean handled() {
        return handled;
    }

    public void markHandled() {
        handled = true;
    }
}
