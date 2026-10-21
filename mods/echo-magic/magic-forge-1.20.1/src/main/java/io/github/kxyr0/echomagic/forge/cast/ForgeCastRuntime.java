package io.github.kxyr0.echomagic.forge.cast;

import io.github.kxyr0.echomagic.api.cast.SpellCastOutcome;
import io.github.kxyr0.echomagic.api.cast.SpellCastStatus;
import io.github.kxyr0.echomagic.api.definition.SpellDefinition;
import io.github.kxyr0.echomagic.api.identity.SpellCastCommandId;
import io.github.kxyr0.echomagic.api.identity.SpellCastId;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.identity.SpellProjectileId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import io.github.kxyr0.echomagic.core.cast.SpellBurstScheduler;
import io.github.kxyr0.echomagic.core.cast.SpellCastInstance;
import io.github.kxyr0.echomagic.core.identity.DeterministicMagicIds;
import io.github.kxyr0.echomagic.forge.api.event.MagicLifecycleEvent;
import io.github.kxyr0.echomagic.forge.api.view.SpellCastReceipt;
import io.github.kxyr0.echomagic.forge.entity.MagicProjectileEntities;
import io.github.kxyr0.echomagic.forge.entity.MagicProjectileEntity;
import io.github.kxyr0.echomagic.forge.entity.MagicProjectilePayload;
import io.github.kxyr0.echomagic.forge.service.ForgeMagicServices;
import io.github.kxyr0.echomagic.forge.sound.MagicSpellSounds;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

public final class ForgeCastRuntime {
    private final ForgeMagicServices services;
    private final SpellBurstScheduler scheduler = new SpellBurstScheduler(4);
    private final Map<SpellCastId, SpellCastInstance> casts = new LinkedHashMap<>();
    private final Map<SpellProjectileId, MagicProjectileEntity> projectiles = new LinkedHashMap<>();
    private final Map<SpellCastCommandId, SpellCastReceipt> commands = new LinkedHashMap<>();
    private final Map<SpellCastId, GestureCastProfile> profiles = new LinkedHashMap<>();

    public ForgeCastRuntime(ForgeMagicServices services) { this.services = services; }

    public SpellCastReceipt cast(MagicPlayerRef ref, Optional<SpellId> requested, SpellCastCommandId command, long tick) {
        return cast(ref, requested, command, tick, GestureCastContext.immediate());
    }

    public SpellCastReceipt cast(MagicPlayerRef ref, Optional<SpellId> requested, SpellCastCommandId command,
                                 long tick, GestureCastContext context) {
        if (commands.containsKey(command)) return commands.get(command);
        ServerPlayer player = services.player(ref).orElse(null);
        if (player == null) return rejected(requested, SpellCastOutcome.PLAYER_NOT_FOUND, tick);
        if (!io.github.kxyr0.echomagic.forge.service.MagicFeatureAccess.server(player.getUUID())) {
            return rejected(requested, SpellCastOutcome.CASTING_BLOCKED, tick);
        }
        Optional<SpellId> selected = services.selectedSpell(ref).selected();
        Optional<SpellId> resolved = requested.isPresent() ? requested : selected;
        if (resolved.isEmpty()) return rejected(Optional.empty(), SpellCastOutcome.SPELL_NOT_SELECTED, tick);
        SpellId spell = resolved.orElseThrow();
        Optional<SpellDefinition> definition = services.find(spell);
        if (definition.isEmpty()) return rejected(Optional.of(spell), SpellCastOutcome.SPELL_NOT_FOUND, tick);
        if (!services.knows(ref, spell)) return rejected(Optional.of(spell), SpellCastOutcome.SPELL_NOT_KNOWN, tick);
        if (!services.cooldownReady(ref, spell, tick)) return rejected(Optional.of(spell), SpellCastOutcome.ON_COOLDOWN, tick);
        if (!player.isAlive() || player.isSpectator()) return rejected(Optional.of(spell), SpellCastOutcome.CASTING_BLOCKED, tick);
        if (activeCasts(ref) >= services.limits().maximumActiveCasts()) {
            return rejected(Optional.of(spell), SpellCastOutcome.CASTING_BLOCKED, tick);
        }
        Vec3 look = player.getLookAngle();
        if (!finiteDirection(look)) return rejected(Optional.of(spell), SpellCastOutcome.INVALID_DIRECTION, tick);
        SpellDefinition original = definition.orElseThrow();
        GestureCastProfile profile = new GestureCastProfile(context, original.burst());
        SpellDefinition runtimeDefinition = profile.runtimeDefinition(original, services.limits().maximumBurst());
        SpellCastId castId = DeterministicMagicIds.cast(command);
        SpellCastInstance instance = new SpellCastInstance(castId, ref, runtimeDefinition, tick,
                profile.firstEmissionTick(tick));
        casts.put(castId, instance);
        profiles.put(castId, profile);
        services.beginCooldown(ref, spell, tick, definition.orElseThrow().cooldown().ticks());
        if (original.cast().swingHand()) player.swing(InteractionHand.MAIN_HAND, true);
        MagicSpellSounds.cast(player.serverLevel(), player.position(), original);
        MagicLifecycleEvent lifecycle = new MagicLifecycleEvent(ref, spell, castId);
        MinecraftForge.EVENT_BUS.post(lifecycle);
        if (lifecycle.handled()) {
            cancel(instance);
            casts.remove(castId);
            profiles.remove(castId);
        }
        SpellCastReceipt receipt = new SpellCastReceipt(Optional.of(castId), Optional.of(spell),
                SpellCastStatus.ACCEPTED, Optional.empty(), tick);
        remember(command, receipt);
        return receipt;
    }

    public void tick(long tick) {
        int emissionBudget = 16;
        while (emissionBudget > 0) {
            var due = scheduler.due(casts.values(), tick);
            if (due.isEmpty()) break;
            for (SpellCastInstance cast : due) {
                emit(cast);
                if (--emissionBudget == 0) break;
            }
        }
        casts.entrySet().removeIf(entry -> {
            boolean terminal = entry.getValue().status() == SpellCastStatus.COMPLETED
                    || entry.getValue().status() == SpellCastStatus.CANCELLED;
            if (terminal) profiles.remove(entry.getKey());
            return terminal;
        });
    }

    public void activate(SpellCastId castId, long tick) {
        SpellCastInstance cast = casts.get(castId);
        if (cast != null) cast.activate(tick);
    }

    public void cancel(SpellCastId castId) {
        SpellCastInstance cast = casts.get(castId);
        if (cast != null) cancel(cast);
    }

    public void retarget(SpellCastId castId, Vec3 origin, Vec3 direction) {
        profiles.computeIfPresent(castId, (id, profile) -> profile.withAim(origin, direction));
    }

    public void terminal(SpellProjectileId projectile) {
        MagicProjectileEntity entity = projectiles.remove(projectile);
        if (entity == null) return;
        casts.values().stream().filter(cast -> cast.activeProjectileIds().contains(projectile)).findFirst()
                .ifPresent(cast -> cast.terminal(projectile));
    }

    public int activeProjectileCount() { return projectiles.size(); }
    public int activeCastCount() { return casts.size(); }

    public void reset() {
        casts.clear();
        projectiles.clear();
        commands.clear();
        profiles.clear();
    }

    private void emit(SpellCastInstance cast) {
        ServerPlayer player = services.player(cast.caster()).orElse(null);
        if (player == null || !player.isAlive()) { cancel(cast); return; }
        if (!io.github.kxyr0.echomagic.forge.service.MagicFeatureAccess.server(player.getUUID())) { cancel(cast); return; }
        if (activeProjectiles(cast.caster()) >= services.limits().maximumActiveProjectiles()) {
            cancel(cast);
            return;
        }
        SpellProjectileId id = DeterministicMagicIds.projectile(cast.id(), cast.emittedCount());
        MagicProjectileEntity entity = MagicProjectileEntities.PROJECTILE.get().create(player.serverLevel());
        if (entity == null) { cancel(cast); return; }
        GestureCastProfile profile = profiles.get(cast.id());
        if (profile == null) { cancel(cast); return; }
        Vec3 direction = profile.direction(player, cast.definition(), cast.emittedCount());
        Vec3 origin = profile.origin(player, cast.definition(), direction);
        entity.configure(MagicProjectilePayload.from(id, cast.id(), player.getUUID(), cast.definition()), origin,
                direction.scale(cast.definition().projectile().speed()));
        if (!player.serverLevel().addFreshEntity(entity)) { cancel(cast); return; }
        projectiles.put(id, entity);
        cast.emitted(id);
    }

    private void cancel(SpellCastInstance cast) {
        cast.activeProjectileIds().forEach(id -> {
            MagicProjectileEntity entity = projectiles.remove(id);
            if (entity != null) entity.discard();
        });
        cast.cancel();
    }

    private int activeCasts(MagicPlayerRef player) { return (int) casts.values().stream().filter(c -> c.caster().equals(player)).count(); }
    private int activeProjectiles(MagicPlayerRef player) {
        return casts.values().stream().filter(c -> c.caster().equals(player)).mapToInt(c -> c.activeProjectileIds().size()).sum();
    }
    private boolean finiteDirection(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z) && value.lengthSqr() > 1.0E-8D;
    }
    private SpellCastReceipt rejected(Optional<SpellId> spell, SpellCastOutcome outcome, long tick) {
        return new SpellCastReceipt(Optional.empty(), spell, SpellCastStatus.FAILED, Optional.of(outcome), tick);
    }
    private void remember(SpellCastCommandId command, SpellCastReceipt receipt) {
        commands.put(command, receipt);
        while (commands.size() > 4096) commands.remove(commands.keySet().iterator().next());
    }
}
