package io.github.kxyr0.echomagic.forge.gesture;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureSymbol;
import io.github.kxyr0.echomagic.api.gesture.MagicGestureModifier;
import io.github.kxyr0.echomagic.api.gesture.MagicTechnique;
import io.github.kxyr0.echomagic.api.definition.SpellDefinition;
import io.github.kxyr0.echomagic.api.identity.SpellCastCommandId;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import io.github.kxyr0.echomagic.core.gesture.MagicGestureMode;
import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;
import io.github.kxyr0.echomagic.core.gesture.MagicGestureRecognizer;
import io.github.kxyr0.echomagic.core.gesture.composition.MagicGestureComposition;
import io.github.kxyr0.echomagic.core.gesture.composition.MagicGestureCompositionRecognizer;
import io.github.kxyr0.echomagic.forge.cast.GestureCastContext;
import io.github.kxyr0.echomagic.forge.network.MagicCastAcceptedPacket;
import io.github.kxyr0.echomagic.forge.network.MagicCastRejectedPacket;
import io.github.kxyr0.echomagic.forge.network.MagicNetwork;
import io.github.kxyr0.echomagic.forge.network.gesture.MagicSchoolSelectedPacket;
import io.github.kxyr0.echomagic.forge.service.ForgeMagicServices;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class ServerGestureCasting {
    private final MagicGestureRecognizer schoolRecognizer = new MagicGestureRecognizer();
    private final MagicGestureCompositionRecognizer compositions = new MagicGestureCompositionRecognizer();
    private final GestureSpellScheduler scheduler = new GestureSpellScheduler();
    private final GestureLinkScheduler links = new GestureLinkScheduler();

    public void cast(ForgeMagicServices services, ServerPlayer player, MagicGestureMode mode,
                     List<List<MagicGesturePoint>> strokes) {
        if (!io.github.kxyr0.echomagic.forge.service.MagicFeatureAccess.server(player.getUUID())) {
            reject(services, player, "CASTING_BLOCKED"); return;
        }
        if (strokes.isEmpty()) {
            reject(services, player, "GESTURE_UNRECOGNIZED");
            return;
        }
        if (mode == MagicGestureMode.SCHOOL) selectSchool(services, player, strokes);
        else castTechnique(services, player, strokes);
    }

    private void selectSchool(ForgeMagicServices services, ServerPlayer player,
                              List<List<MagicGesturePoint>> strokes) {
        List<MagicGesturePoint> points = MagicGestureRecognizer.mergeStrokes(strokes);
        MagicPlayerRef ref = new MagicPlayerRef(player.getUUID());
        var personal = services.gestureTraining().recognizeDetailed(ref, MagicGestureMode.SCHOOL, points);
        if (personal.ambiguous()) { reject(services, player, "GESTURE_AMBIGUOUS"); return; }
        Optional<MagicGestureSymbol> recognized = personal.target().flatMap(target -> target.symbol());
        if (recognized.isEmpty()) recognized = schoolRecognizer.recognize(points).symbol();
        if (recognized.isEmpty()) {
            reject(services, player, "GESTURE_UNRECOGNIZED");
            return;
        }
        MagicGestureSymbol selected = recognized.orElseThrow();
        services.selectGestureSchool(ref, selected);
        MagicNetwork.send(player, new MagicSchoolSelectedPacket(selected.serializedName()));
    }

    private void castTechnique(ForgeMagicServices services, ServerPlayer player,
                               List<List<MagicGesturePoint>> strokes) {
        MagicPlayerRef ref = new MagicPlayerRef(player.getUUID());
        Optional<MagicGestureSymbol> activeSchool = services.gestureSchool(ref);
        if (activeSchool.isEmpty()) {
            reject(services, player, "GESTURE_SCHOOL_REQUIRED");
            return;
        }
        var personal = services.gestureTraining().recognizeDetailed(ref, MagicGestureMode.TECHNIQUE, strokes.get(0));
        if (personal.ambiguous()) { reject(services, player, "GESTURE_AMBIGUOUS"); return; }
        Optional<MagicTechnique> personalTechnique = personal.target().flatMap(target -> target.technique());
        Optional<MagicGestureComposition> composition = compositions.recognize(strokes, personalTechnique);
        if (composition.isEmpty()) {
            reject(services, player, "GESTURE_TECHNIQUE_UNRECOGNIZED");
            return;
        }
        MagicGestureComposition recognized = composition.orElseThrow();
        castSpell(services, player, ref, activeSchool.orElseThrow(), recognized);
    }

    private void castSpell(ForgeMagicServices services, ServerPlayer player, MagicPlayerRef ref,
                           MagicGestureSymbol school, MagicGestureComposition composition) {
        MagicTechnique spellTechnique = composition.technique() == MagicTechnique.HOOK
                ? MagicTechnique.LINE : composition.technique();
        Optional<LivingEntity> linkedTarget = linkedTarget(services, player, composition);
        if (composition.technique() == MagicTechnique.HOOK && linkedTarget.isEmpty()) return;
        List<SpellId> candidates = services.knownSpells(ref).spells().stream()
                .filter(id -> services.find(id).map(definition -> school.matches(definition.school())
                        && definition.technique() == spellTechnique).orElse(false))
                .sorted(Comparator.naturalOrder()).toList();
        if (candidates.isEmpty() && knowsSchool(services, ref, school)) {
            candidates = services.catalog().definitions().stream()
                    .filter(definition -> school.matches(definition.school())
                            && definition.technique() == spellTechnique)
                    .map(definition -> definition.id()).sorted().toList();
            candidates.forEach(id -> services.grant(ref, id));
        }
        if (candidates.isEmpty()) {
            reject(services, player, "GESTURE_TECHNIQUE_UNAVAILABLE");
            return;
        }
        SpellId spell = services.selectedSpell(ref).selected().filter(candidates::contains).orElse(candidates.get(0));
        SpellDefinition definition = services.find(spell).orElseThrow();
        GestureCastContext context = GestureCastContextFactory.create(
                services, player, definition, composition, linkedTarget);
        SpellCastCommandId command = new SpellCastCommandId("gesture:" + player.getUUID() + ':'
                + services.serverTick() + ':' + System.nanoTime());
        var receipt = services.castGestureSpell(ref, spell, command, context);
        if (receipt.accepted()) {
            schedule(services, player, ref, school, definition, composition, context, linkedTarget,
                    receipt.castId().orElseThrow());
            MagicNetwork.send(player, new MagicCastAcceptedPacket(receipt.castId().orElseThrow().value(),
                    receipt.spellId().orElseThrow().value(), services.serverTick()));
        } else {
            MagicNetwork.send(player, new MagicCastRejectedPacket(receipt.outcome().orElseThrow().name()));
        }
        services.syncPlayer(player);
    }

    private Optional<LivingEntity> linkedTarget(ForgeMagicServices services, ServerPlayer player,
                                                 MagicGestureComposition composition) {
        if (composition.technique() != MagicTechnique.HOOK) return Optional.empty();
        if (composition.modifier() != MagicGestureModifier.DELAY) {
            reject(services, player, "GESTURE_HOOK_REQUIRES_POINT");
            return Optional.empty();
        }
        Optional<LivingEntity> target = links.findTarget(services, player);
        if (target.isEmpty()) reject(services, player, "GESTURE_TARGET_REQUIRED");
        return target;
    }

    public void tick(ForgeMagicServices services, long tick) {
        scheduler.tick(services, tick); links.tick(services, tick);
    }
    public void reset() { scheduler.reset(); links.reset(); }
    public void playerLeft(ForgeMagicServices services, MagicPlayerRef player) {
        scheduler.playerLeft(services, player); links.playerLeft(services, player);
    }

    private void schedule(ForgeMagicServices services, ServerPlayer player, MagicPlayerRef ref,
                          MagicGestureSymbol school, SpellDefinition definition,
                          MagicGestureComposition composition, GestureCastContext context,
                          Optional<LivingEntity> linkedTarget,
                          io.github.kxyr0.echomagic.api.identity.SpellCastId castId) {
        if (composition.technique() == MagicTechnique.HOOK) {
            links.link(services, ref, castId, school, player.serverLevel(), context.origin().orElseThrow(),
                    linkedTarget.orElseThrow(), services.serverTick());
            return;
        }
        if (composition.modifier() != MagicGestureModifier.DELAY) return;
        var origin = context.origin().orElseThrow();
        if (composition.technique() == MagicTechnique.CIRCLE) {
            scheduler.seal(ref, castId, school, player.serverLevel(), origin, services.serverTick(),
                    definition.friendlyFire());
        } else {
            scheduler.delayed(ref, castId, school, player.serverLevel(), origin,
                    services.serverTick() + context.initialDelayTicks(), definition.friendlyFire());
        }
    }

    private static boolean knowsSchool(ForgeMagicServices services, MagicPlayerRef player,
                                       MagicGestureSymbol school) {
        return services.knownSpells(player).spells().stream().anyMatch(id -> services.find(id)
                .map(definition -> school.matches(definition.school())).orElse(false));
    }

    private static void reject(ForgeMagicServices services, ServerPlayer player, String reason) {
        MagicNetwork.send(player, new MagicCastRejectedPacket(reason));
        services.syncPlayer(player);
    }
}
