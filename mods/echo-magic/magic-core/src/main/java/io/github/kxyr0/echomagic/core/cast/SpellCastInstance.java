package io.github.kxyr0.echomagic.core.cast;

import io.github.kxyr0.echomagic.api.cast.SpellCastStatus;
import io.github.kxyr0.echomagic.api.definition.SpellDefinition;
import io.github.kxyr0.echomagic.api.identity.SpellCastId;
import io.github.kxyr0.echomagic.api.identity.SpellProjectileId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class SpellCastInstance {
    private final SpellCastId id;
    private final MagicPlayerRef caster;
    private final SpellDefinition definition;
    private final Set<SpellProjectileId> active = new LinkedHashSet<>();
    private SpellCastStatus status = SpellCastStatus.ACCEPTED;
    private int emitted;
    private long nextEmissionTick;

    public SpellCastInstance(SpellCastId id, MagicPlayerRef caster, SpellDefinition definition, long acceptedTick) {
        this(id, caster, definition, acceptedTick, acceptedTick);
    }

    public SpellCastInstance(SpellCastId id, MagicPlayerRef caster, SpellDefinition definition, long acceptedTick,
                             long initialEmissionTick) {
        this.id = Objects.requireNonNull(id, "id");
        this.caster = Objects.requireNonNull(caster, "caster");
        this.definition = Objects.requireNonNull(definition, "definition");
        if (initialEmissionTick < acceptedTick) {
            throw new IllegalArgumentException("Initial emission must not precede cast acceptance.");
        }
        nextEmissionTick = initialEmissionTick;
    }

    public boolean emissionDue(long tick) { return emitted < definition.burst().count() && tick >= nextEmissionTick; }
    public void emitted(SpellProjectileId projectile) {
        if (emitted >= definition.burst().count()) throw new IllegalStateException("Burst is complete.");
        active.add(projectile);
        emitted++;
        nextEmissionTick += definition.burst().intervalTicks();
        status = emitted < definition.burst().count() ? SpellCastStatus.EMITTING : SpellCastStatus.ACTIVE_PROJECTILES;
    }
    public void terminal(SpellProjectileId projectile) {
        active.remove(projectile);
        if (emitted == definition.burst().count() && active.isEmpty()) status = SpellCastStatus.COMPLETED;
    }
    public void cancel() { status = SpellCastStatus.CANCELLED; }
    public void activate(long tick) {
        if (nextEmissionTick == Long.MAX_VALUE && status == SpellCastStatus.ACCEPTED) nextEmissionTick = tick;
    }
    public SpellCastId id() { return id; }
    public MagicPlayerRef caster() { return caster; }
    public SpellDefinition definition() { return definition; }
    public SpellCastStatus status() { return status; }
    public int emittedCount() { return emitted; }
    public long nextEmissionTick() { return nextEmissionTick; }
    public Set<SpellProjectileId> activeProjectileIds() { return Set.copyOf(active); }
}
