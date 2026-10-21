package io.github.kxyr0.echonpcbackpack.animation;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.marker.AnimationMarkerDefinition;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.api.AnimationService;
import io.github.kxyr0.echoanimation.forge.api.AnimationServiceBridge;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPhase;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionType;
import io.github.kxyr0.echonpcbackpack.api.view.NpcBackpackView;
import io.github.kxyr0.echonpcbackpack.core.transition.BackpackMarkerSequence;
import io.github.kxyr0.echonpcbackpack.forge.api.NpcBackpackServiceBridge;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.server.MinecraftServer;

public final class BackpackAnimationStarter {
    private final BackpackAnimationBindingCatalog catalog;
    private final Set<String> startedTransitions = new LinkedHashSet<>();

    public BackpackAnimationStarter(BackpackAnimationBindingCatalog catalog) {
        this.catalog = catalog;
    }

    public void tick(MinecraftServer server) {
        var backpacks = NpcBackpackServiceBridge.lookup(server);
        var animations = AnimationServiceBridge.lookup(server);
        var npcs = EchoNpcServiceBridge.lookup(server);
        if (backpacks.status() != NpcBackpackServiceBridge.Status.RESOLVED
                || animations.status() != io.github.kxyr0.echoanimation.forge.api.AnimationServiceLookupResult.Status.RESOLVED
                || npcs.status() != io.github.kxyr0.echonpc.forge.api.EchoNpcServiceLookupResult.Status.RESOLVED) {
            return;
        }
        backpacks.service().orElseThrow().reservations().stream().limit(64).forEach(reservation ->
                backpacks.service().orElseThrow().inspect(reservation.actorId()).ifPresent(view ->
                        startIfReady(animations.service().orElseThrow(), npcs.service().orElseThrow(), view)));
    }

    public void clear() {
        startedTransitions.clear();
    }

    private void startIfReady(AnimationService service,
            io.github.kxyr0.echonpc.api.service.NpcService npcs, NpcBackpackView view) {
        var transition = view.transition();
        if (transition.isEmpty() || !ready(transition.orElseThrow().phase())) return;
        String transitionId = transition.orElseThrow().id().value();
        if (startedTransitions.contains(transitionId)) return;
        NpcBackpackTransitionType type = transition.orElseThrow().type();
        String activeForm = npcs.getForm(view.actorId()).map(form -> form.value()).orElse("*");
        AnimationId animation = catalog.resolve(view.actorId(), activeForm, type).orElse(null);
        if (animation == null || !valid(service, animation, type)) return;
        AnimationSubjectId subject = new AnimationSubjectId(view.actorId().value());
        if (!service.ensureSubject(subject)) return;
        var result = service.playAnimation(animation, subject,
                AnimationCommandId.parse("echo_npc_sophisticated_backpacks:" + path(transitionId)));
        if (result.status() == io.github.kxyr0.echoanimation.api.animation.AnimationPlayResult.Status.STARTED) {
            startedTransitions.add(transitionId);
            while (startedTransitions.size() > 1024) {
                startedTransitions.remove(startedTransitions.iterator().next());
            }
        }
    }

    private boolean valid(AnimationService service, AnimationId animation, NpcBackpackTransitionType type) {
        if (!service.animations().contains(animation)) return false;
        List<String> required = BackpackMarkerSequence.required(type);
        List<AnimationMarkerDefinition> markers = service.animationMarkers(animation).stream()
                .sorted(Comparator.comparingInt((AnimationMarkerDefinition marker) -> phase(marker.phase()))
                        .thenComparingLong(AnimationMarkerDefinition::phaseTick)).toList();
        List<String> present = markers.stream().map(marker -> marker.id().value())
                .filter(required::contains).toList();
        return present.equals(required) && new HashSet<>(present).size() == required.size();
    }

    private boolean ready(NpcBackpackTransitionPhase phase) {
        return phase == NpcBackpackTransitionPhase.READY_TO_TAKE
                || phase == NpcBackpackTransitionPhase.ATTACHED_TO_BACK;
    }

    private int phase(AnimationPhase phase) {
        return switch (phase) { case ENTER -> 0; case MAIN -> 1; case EXIT -> 2; case TERMINAL -> 3; };
    }

    private String path(String value) {
        return value.substring(value.indexOf(':') + 1).replace('/', '_').replace('.', '_');
    }
}
