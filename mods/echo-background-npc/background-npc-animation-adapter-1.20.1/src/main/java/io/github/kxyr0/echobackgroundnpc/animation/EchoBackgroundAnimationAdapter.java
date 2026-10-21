package io.github.kxyr0.echobackgroundnpc.animation;

import io.github.kxyr0.echobackgroundnpc.api.runtime.BackgroundAgentState;
import io.github.kxyr0.echobackgroundnpc.api.task.BackgroundTask;
import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.profile.AnimationProfileId;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectLifecycleOwnerId;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectProvenance;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectProvisioningRequest;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.api.AnimationServiceBridge;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

/** Maps high-level background tasks onto the existing Echo Animation public service. */
public final class EchoBackgroundAnimationAdapter {
    private static final AnimationSubjectLifecycleOwnerId OWNER =
            new AnimationSubjectLifecycleOwnerId("echo_background_npc:runtime");

    public Result transition(MinecraftServer server, BackgroundAgentState state, String profileId) {
        return play(server, state, profileId, animation(state));
    }

    public Result conversation(MinecraftServer server, BackgroundAgentState state, String profileId,
            boolean speaking) {
        return play(server, state, profileId, speaking ? "echo_background_npc:talk" : null);
    }

    private Result play(MinecraftServer server, BackgroundAgentState state, String profileId, String animation) {
        var lookup = AnimationServiceBridge.lookup(server);
        if (lookup.service().isEmpty()) return new Result(Status.UNAVAILABLE, "Animation adapter unavailable.");
        var service = lookup.service().orElseThrow();
        AnimationSubjectId subject = new AnimationSubjectId(state.id().value());
        AnimationCommandId command = new AnimationCommandId("echo_background_npc:task_" + state.revision());
        var request = new AnimationSubjectProvisioningRequest(subject, new AnimationProfileId(profileId),
                new AnimationSubjectProvenance(OWNER, state.id().value()), command);
        var provisioning = service.ensureSubject(request);
        if (provisioning.status() != io.github.kxyr0.echoanimation.api.provisioning
                .AnimationSubjectProvisioningResult.Status.CREATED
                && provisioning.status() != io.github.kxyr0.echoanimation.api.provisioning
                .AnimationSubjectProvisioningResult.Status.EXISTING && !service.ensureSubject(subject)) {
            return new Result(Status.FAILED, provisioning.message());
        }
        if (animation == null) {
            service.stopAnimation(subject, Optional.empty(), command);
            return new Result(Status.PLAYING, "Idle pose selected.");
        }
        var playback = service.playAnimation(new AnimationId(animation), subject, command);
        return playback.playbackId().isPresent() ? new Result(Status.PLAYING, playback.message())
                : new Result(Status.FAILED, playback.message());
    }

    public void retire(MinecraftServer server, BackgroundAgentState state) {
        AnimationServiceBridge.lookup(server).service().ifPresent(service -> service.retireSubject(
                new AnimationSubjectId(state.id().value()), OWNER));
    }

    private String animation(BackgroundAgentState state) {
        BackgroundTask task = state.task();
        if (task instanceof BackgroundTask.Perform perform) return perform.animationId();
        return switch (task.kind()) {
            case TRAVEL -> state.routeProgress() < 1.0D ? "jdh:humanoid_walk" : "jdh:humanoid_idle";
            case CONVERSE -> "echo_background_npc:talk";
            case STAY, PERFORM -> null;
        };
    }

    public record Result(Status status, String message) { }
    public enum Status { PLAYING, UNAVAILABLE, FAILED }
}
