package io.github.kxyr0.echomagic.forge.service;

import io.github.kxyr0.echomagic.api.definition.SpellDefinition;
import io.github.kxyr0.echomagic.api.gesture.MagicGestureSymbol;
import io.github.kxyr0.echomagic.api.diagnostic.MagicDiagnostic;
import io.github.kxyr0.echomagic.api.identity.SpellCastCommandId;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.identity.SpellProjectileId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import io.github.kxyr0.echomagic.api.view.KnownSpellView;
import io.github.kxyr0.echomagic.api.view.SelectedSpellView;
import io.github.kxyr0.echomagic.api.view.SpellCatalogView;
import io.github.kxyr0.echomagic.api.view.SpellCooldownView;
import io.github.kxyr0.echomagic.core.catalog.SpellCatalog;
import io.github.kxyr0.echomagic.core.hit.MagicHitLedger;
import io.github.kxyr0.echomagic.core.gesture.MagicGestureMode;
import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;
import io.github.kxyr0.echomagic.core.player.KnownSpellRegistry;
import io.github.kxyr0.echomagic.core.player.SelectedSpellRegistry;
import io.github.kxyr0.echomagic.core.player.SpellCooldownRegistry;
import io.github.kxyr0.echomagic.core.validation.SpellDefinitionValidator;
import io.github.kxyr0.echomagic.core.validation.SpellSafetyLimits;
import io.github.kxyr0.echomagic.forge.api.MagicService;
import io.github.kxyr0.echomagic.forge.api.diagnostic.MagicDiagnosticsView;
import io.github.kxyr0.echomagic.forge.api.view.SpellCastReceipt;
import io.github.kxyr0.echomagic.forge.cast.ForgeCastRuntime;
import io.github.kxyr0.echomagic.forge.cast.GestureCastContext;
import io.github.kxyr0.echomagic.forge.config.MagicServerConfig;
import io.github.kxyr0.echomagic.forge.content.ForgeSpellContentResult;
import io.github.kxyr0.echomagic.forge.damage.MagicDamageExecutor;
import io.github.kxyr0.echomagic.forge.entity.MagicProjectileEntity;
import io.github.kxyr0.echomagic.forge.entity.MagicProjectilePayload;
import io.github.kxyr0.echomagic.forge.gesture.ServerGestureCasting;
import io.github.kxyr0.echomagic.forge.gesture.GestureSchoolRegistry;
import io.github.kxyr0.echomagic.forge.gesture.GestureTrainingRegistry;
import io.github.kxyr0.echomagic.forge.network.MagicImpactPacket;
import io.github.kxyr0.echomagic.forge.network.MagicNetwork;
import io.github.kxyr0.echomagic.forge.network.MagicPlayerStateSnapshotPacket;
import io.github.kxyr0.echomagic.forge.network.gesture.MagicGestureTemplatesPacket;
import io.github.kxyr0.echomagic.forge.persistence.MagicPlayerSavedData;
import io.github.kxyr0.echomagic.forge.persistence.PlayerMagicPersistence;
import io.github.kxyr0.echomagic.forge.target.MagicTargetFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class ForgeMagicServices implements MagicService, MagicDiagnosticsView {
    private final SpellCatalog spells = new SpellCatalog(new SpellDefinitionValidator(MagicServerConfig::limits));
    private final KnownSpellRegistry known = new KnownSpellRegistry(() -> MagicServerConfig.limits().maximumKnownSpells());
    private final SelectedSpellRegistry selected = new SelectedSpellRegistry();
    private final SpellCooldownRegistry cooldowns = new SpellCooldownRegistry();
    private final MagicHitLedger hits = new MagicHitLedger(4096);
    private final MagicDamageExecutor damage = new MagicDamageExecutor(hits);
    private final MagicTargetFilter targetFilter = new MagicTargetFilter();
    private final ForgeCastRuntime casting = new ForgeCastRuntime(this);
    private final List<MagicDiagnostic> diagnostics = new ArrayList<>();
    private final Set<MagicPlayerRef> restored = new HashSet<>();
    private long serverTick, syncRevision;
    private final ServerGestureCasting gestureCasting = new ServerGestureCasting();
    private final GestureSchoolRegistry gestureSchools = new GestureSchoolRegistry();
    private final GestureTrainingRegistry gestureTraining = new GestureTrainingRegistry();

    public void accept(ForgeSpellContentResult content) {
        diagnostics.addAll(content.diagnostics());
        if (content.diagnostics().stream().anyMatch(MagicDiagnostic::isError)) { trimDiagnostics(); return; }
        var published = spells.publish(content.definitions());
        diagnostics.addAll(published.diagnostics());
        if (published.published()) cleanupMissingSelections();
        trimDiagnostics(); }

    @Override public boolean grant(MagicPlayerRef player, SpellId spell) {
        if (find(spell).isEmpty()) return false;
        boolean changed = known.grant(player, spell);
        persistAndSync(player);
        return changed; }
    @Override public boolean remove(MagicPlayerRef player, SpellId spell) {
        boolean changed = known.remove(player, spell);
        selected.clearIf(player, spell);
        persistAndSync(player);
        return changed; }
    @Override public boolean knows(MagicPlayerRef player, SpellId spell) { return known.knows(player, spell); }
    @Override public boolean select(MagicPlayerRef player, SpellId spell) {
        boolean changed = find(spell).isPresent() && selected.select(player, spell, known);
        if (changed) persistAndSync(player);
        return changed; }
    @Override public void clearSelection(MagicPlayerRef player) { selected.clear(player); persistAndSync(player); }
    @Override public SpellCastReceipt cast(MagicPlayerRef player, Optional<SpellId> spell, SpellCastCommandId command) {
        restore(player);
        return casting.cast(player, spell, command, serverTick); }
    @Override public Optional<SpellDefinition> find(SpellId id) { return spells.find(id); }
    @Override public SpellCatalogView catalog() {
        var snapshot = spells.snapshot();
        return new SpellCatalogView(snapshot.definitions(), snapshot.revision()); }
    @Override public KnownSpellView knownSpells(MagicPlayerRef player) { return known.view(player); }
    @Override public SelectedSpellView selectedSpell(MagicPlayerRef player) { return selected.view(player); }
    @Override public SpellCooldownView cooldown(MagicPlayerRef player, SpellId spell) {
        return cooldowns.view(player, spell, serverTick); }
    @Override public void projectileTerminal(SpellProjectileId projectileId) { casting.terminal(projectileId); }
    @Override public int activeProjectileCount() { return casting.activeProjectileCount(); }
    @Override public List<MagicDiagnostic> diagnostics() { return List.copyOf(diagnostics); }

    public void tick() { serverTick++; gestureCasting.tick(this, serverTick); casting.tick(serverTick); }
    public void serverStarted() { serverTick = 0L; restored.clear(); casting.reset(); gestureCasting.reset(); gestureTraining.reset(); targetFilter.refreshProviders(); }
    public long serverTick() { return serverTick; }
    public int activeCastCount() { return casting.activeCastCount(); }
    public SpellSafetyLimits limits() { return MagicServerConfig.limits(); }
    public MagicDamageExecutor damage() { return damage; }
    public MagicTargetFilter targetFilter() { return targetFilter; }
    public Optional<ServerPlayer> player(MagicPlayerRef ref) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? Optional.empty() : Optional.ofNullable(server.getPlayerList().getPlayer(ref.uuid())); }
    public boolean cooldownReady(MagicPlayerRef player, SpellId spell, long tick) { return cooldowns.ready(player, spell, tick); }
    public void beginCooldown(MagicPlayerRef player, SpellId spell, long tick, long duration) {
        cooldowns.begin(player, spell, tick, duration); persistAndSync(player); }
    @Override public void clearCooldown(MagicPlayerRef player, SpellId spell) {
        cooldowns.clear(player, spell); persistAndSync(player); }

    public void castGesture(ServerPlayer player, MagicGestureMode mode, List<List<MagicGesturePoint>> strokes) {
        gestureCasting.cast(this, player, mode, strokes); }
    public SpellCastReceipt castGestureSpell(MagicPlayerRef player, SpellId spell, SpellCastCommandId command, GestureCastContext context) {
        return casting.cast(player, Optional.of(spell), command, serverTick, context); }
    public void activateGestureCast(io.github.kxyr0.echomagic.api.identity.SpellCastId cast, long tick) { casting.activate(cast, tick); }
    public void cancelGestureCast(io.github.kxyr0.echomagic.api.identity.SpellCastId cast) { casting.cancel(cast); }
    public void retargetGestureCast(io.github.kxyr0.echomagic.api.identity.SpellCastId cast, Vec3 origin, Vec3 direction) { casting.retarget(cast, origin, direction); }
    public Optional<MagicGestureSymbol> gestureSchool(MagicPlayerRef player) { return gestureSchools.school(player); }
    public void selectGestureSchool(MagicPlayerRef player, MagicGestureSymbol school) {
        gestureSchools.select(player, school); persistAndSync(player);
    }
    public GestureTrainingRegistry gestureTraining() { return gestureTraining; }
    public void persistPlayer(MagicPlayerRef player) { persistAndSync(player); }

    public void restoreAndSync(ServerPlayer player) { MagicPlayerRef ref = new MagicPlayerRef(player.getUUID()); restore(ref); sync(player); }
    public void impact(MagicProjectileEntity entity, Vec3 point, MagicProjectilePayload payload) {
        MagicNetwork.sendTracking(entity, new MagicImpactPacket(point.x, point.y, point.z, payload.particleStyle(),
                payload.coreRed(), payload.coreGreen(), payload.coreBlue(), payload.vanillaCoreRed(), payload.vanillaCoreGreen(), payload.vanillaCoreBlue())); }

    private void restore(MagicPlayerRef player) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !restored.add(player)) return;
        PlayerMagicPersistence data = MagicPlayerSavedData.get(server).player(player.uuid());
        known.restore(new KnownSpellView(player, data.known(), data.knownRevision()));
        selected.restore(new SelectedSpellView(player, data.selected(), data.selectedRevision()));
        cooldowns.restoreRemaining(player, data.cooldownRemainingTicks(), serverTick);
        gestureSchools.restore(player, data.gestureSchool());
        gestureTraining.restore(player, data.gestureTemplates());
        selected.view(player).selected().filter(id -> find(id).isEmpty() || !known.knows(player, id))
                .ifPresent(id -> selected.clear(player));
    }

    private void persistAndSync(MagicPlayerRef player) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            KnownSpellView knownView = known.view(player);
            SelectedSpellView selectedView = selected.view(player);
            MagicPlayerSavedData.get(server).put(player.uuid(), new PlayerMagicPersistence(knownView.spells(),
                    selectedView.selected(), cooldowns.remainingTicks(player, serverTick), gestureSchools.school(player),
                    gestureTraining.templates(player), knownView.revision(), selectedView.revision()));
        }
        player(player).ifPresent(this::sync);
    }

    private void sync(ServerPlayer player) {
        MagicPlayerRef ref = new MagicPlayerRef(player.getUUID());
        KnownSpellView knownView = known.view(ref);
        SelectedSpellView selectedView = selected.view(ref);
        long remaining = selectedView.selected().map(id -> cooldowns.view(ref, id, serverTick).remainingTicks()).orElse(0L);
        MagicNetwork.send(player, new MagicPlayerStateSnapshotPacket(knownView.spells().stream().sorted()
                .map(SpellId::value).toList(), selectedView.selected().map(SpellId::value),
                gestureSchools.school(ref).map(MagicGestureSymbol::serializedName), remaining, ++syncRevision));
        MagicNetwork.send(player, new MagicGestureTemplatesPacket(gestureTraining.templates(ref)));
    }
    public void syncPlayer(ServerPlayer player) { sync(player); }
    public void playerLeft(ServerPlayer player) { MagicPlayerRef ref = new MagicPlayerRef(player.getUUID());
        gestureCasting.playerLeft(this, ref); persistAndSync(ref); gestureTraining.playerLeft(ref); restored.remove(ref); }

    private void cleanupMissingSelections() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        server.getPlayerList().getPlayers().forEach(player -> {
            MagicPlayerRef ref = new MagicPlayerRef(player.getUUID());
            selected.view(ref).selected().filter(id -> find(id).isEmpty()).ifPresent(id -> clearSelection(ref));
        });
    }
    private void trimDiagnostics() { while (diagnostics.size() > 1024) diagnostics.remove(0); }
}
