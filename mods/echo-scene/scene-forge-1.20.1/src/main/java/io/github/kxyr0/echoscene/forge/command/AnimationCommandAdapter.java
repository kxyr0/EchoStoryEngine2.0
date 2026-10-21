package io.github.kxyr0.echoscene.forge.command;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlayResult;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackOutcome;
import io.github.kxyr0.echoanimation.api.animation.query.AnimationPlaybackQueryResult;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.api.AnimationService;
import io.github.kxyr0.echoanimation.forge.api.AnimationServiceBridge;
import io.github.kxyr0.echoscene.api.command.SceneCommandRequest;
import io.github.kxyr0.echoscene.api.command.SceneCommandStart;
import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import io.github.kxyr0.echoscene.api.identity.SceneStepId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;

final class AnimationCommandAdapter {
    private final Function<SceneExecutionId, ServerPlayer> players;
    private final Map<String, Pending> pending = new LinkedHashMap<>();

    AnimationCommandAdapter(Function<SceneExecutionId, ServerPlayer> players) { this.players = players; }

    SceneCommandStart start(SceneCommandRequest request) {
        ServerPlayer player = players.apply(request.executionId());
        AnimationService service = service(player).orElse(null);
        if (service == null) return SceneCommandStart.failed("Echo Animation service unavailable");
        String key = SceneCommandHub.key(request);
        AnimationId animation = AnimationId.parse(request.command().arguments().get(0));
        AnimationSubjectId subject = new AnimationSubjectId(request.command().receiver());
        if (!service.ensureSubject(subject)) return SceneCommandStart.failed("Animation subject unavailable: "
                + subject.value());
        AnimationPlayResult result = service.playAnimation(animation, subject, AnimationCommandId.parse(key));
        result.playbackId().ifPresent(id ->
                io.github.kxyr0.echoscene.forge.command.context.SceneEventContexts.capture(request, player, id.value()));
        if (result.status() != AnimationPlayResult.Status.STARTED
                && result.status() != AnimationPlayResult.Status.ALREADY_APPLIED) {
            return SceneCommandStart.failed(result.diagnosticCode() + " " + result.message());
        }
        AnimationPlaybackQueryResult query = service.queryAnimation(subject, animation);
        if (query.status() == AnimationPlaybackQueryResult.Status.TERMINAL) {
            return terminal(query) ? SceneCommandStart.completed() : SceneCommandStart.failed(query.message());
        }
        pending.put(key, new Pending(key, request.executionId(), request.command().stepId(), animation, subject,
                player));
        return SceneCommandStart.waiting(key);
    }

    void tick(SceneCommandHub.Completion completion) {
        for (Pending value : List.copyOf(pending.values())) {
            AnimationService service = service(value.player()).orElse(null);
            if (service == null) continue;
            AnimationPlaybackQueryResult result = service.queryAnimation(value.subject(), value.animation());
            if (result.status() != AnimationPlaybackQueryResult.Status.TERMINAL) continue;
            boolean succeeded = terminal(result);
            pending.remove(value.key());
            completion.complete(value.execution(), value.step(), succeeded,
                    succeeded ? "" : result.diagnosticCode() + " " + result.message());
        }
    }

    void cancelOwned(SceneExecutionId owner) {
        pending.values().removeIf(value -> {
            if (!value.execution().equals(owner)) return false;
            service(value.player()).ifPresent(service -> service.stopAnimation(value.subject(),
                    Optional.of(value.animation()), AnimationCommandId.parse(value.key() + "_cancel")));
            return true;
        });
    }

    private Optional<AnimationService> service(ServerPlayer player) {
        return player == null ? Optional.empty() : AnimationServiceBridge.lookup(player.getServer()).service();
    }

    private boolean terminal(AnimationPlaybackQueryResult result) {
        return result.outcome().filter(value -> value == AnimationPlaybackOutcome.COMPLETED
                || value == AnimationPlaybackOutcome.GRACEFULLY_STOPPED).isPresent();
    }

    private record Pending(String key, SceneExecutionId execution, SceneStepId step, AnimationId animation,
                           AnimationSubjectId subject, ServerPlayer player) { }
}
