package io.github.kxyr0.echonpcanimationadapter.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlayResult;
import io.github.kxyr0.echoanimation.api.animation.AnimationStopResult;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectProvisioningResult;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.api.AnimationService;
import io.github.kxyr0.echoanimation.forge.api.AnimationServiceBridge;
import io.github.kxyr0.echoanimation.forge.api.AnimationServiceLookupResult;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceLookupResult;
import io.github.kxyr0.echonpcanimationadapter.AnimationSubjectMapping;
import io.github.kxyr0.echonpcanimationadapter.lifecycle.NpcAnimationLifecycleAdapter;
import io.github.kxyr0.echonpcanimationadapter.resource.NpcAnimationBindingCatalogState;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;

/**
 * Production command bridge from Echo NPC ActorId to Echo Animation subjects.
 */
public final class AdapterAnimationCommands {
    private final AnimationSubjectMapping mapping = new AnimationSubjectMapping();
    private final NpcAnimationBindingCatalogState bindings;
    private final NpcAnimationLifecycleAdapter lifecycle;

    public AdapterAnimationCommands(
            NpcAnimationBindingCatalogState bindings,
            NpcAnimationLifecycleAdapter lifecycle
    ) {
        this.bindings = java.util.Objects.requireNonNull(bindings, "bindings");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_animation")
                .then(Commands.literal("play_animation").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("animation_id", ResourceLocationArgument.id())
                                .then(Commands.argument("npc_id", ResourceLocationArgument.id())
                                        .executes(this::play))))
                .then(Commands.literal("stop_animation").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("npc_id", ResourceLocationArgument.id())
                                .executes(context -> stop(context, Optional.empty()))
                                .then(Commands.argument("animation_id", ResourceLocationArgument.id())
                                        .executes(context -> stop(context, Optional.of(animationId(context))))))));
        new AdapterDiagnosticsCommands(bindings, lifecycle).register(dispatcher);
    }

    private int play(CommandContext<CommandSourceStack> context) {
        ActorId actorId = actorId(context, "npc_id");
        Optional<NpcService> npcService = npcService(context.getSource());
        Optional<NpcActorView> actor = npcService.flatMap(service -> actor(context.getSource(), service, actorId));
        if (actor.isEmpty()) {
            return 0;
        }
        Optional<AnimationService> animationService = animationService(context.getSource());
        if (animationService.isEmpty()) {
            return 0;
        }
        AnimationId animationId = animationId(context);
        AnimationSubjectId subjectId = mapping.subjectId(actorId);
        AnimationSubjectProvisioningResult provisioning = lifecycle.ensureActorSubject(actor.orElseThrow(),
                animationService.orElseThrow());
        if (provisioning.status() == AnimationSubjectProvisioningResult.Status.PROVENANCE_CONFLICT
                || provisioning.status() == AnimationSubjectProvisioningResult.Status.PROFILE_NOT_FOUND
                || provisioning.status() == AnimationSubjectProvisioningResult.Status.SERVICE_UNAVAILABLE) {
            context.getSource().sendFailure(Component.literal("Echo NPC Animation provisioning "
                    + provisioning.status() + " code=" + provisioning.diagnosticCode()
                    + " message=" + provisioning.message()));
            return 0;
        }
        AnimationPlayResult result = animationService.orElseThrow()
                .playAnimation(animationId, subjectId, commandId("play"));
        AdapterCommandFeedback.sendPlayResult(context.getSource(), actorId, result);
        return result.status() == AnimationPlayResult.Status.STARTED
                || result.status() == AnimationPlayResult.Status.ALREADY_APPLIED ? 1 : 0;
    }

    private int stop(CommandContext<CommandSourceStack> context, Optional<AnimationId> animationId) {
        ActorId actorId = actorId(context, "npc_id");
        Optional<NpcService> npcService = npcService(context.getSource());
        if (npcService.isEmpty() || actor(context.getSource(), npcService.orElseThrow(), actorId).isEmpty()) {
            return 0;
        }
        Optional<AnimationService> animationService = animationService(context.getSource());
        if (animationService.isEmpty()) {
            return 0;
        }
        AnimationSubjectId subjectId = mapping.subjectId(actorId);
        AnimationStopResult result = animationService.orElseThrow()
                .stopAnimation(subjectId, animationId, commandId("stop"));
        AdapterCommandFeedback.sendStopResult(context.getSource(), actorId, result);
        return result.status() == AnimationStopResult.Status.STOP_REQUESTED
                || result.status() == AnimationStopResult.Status.ALREADY_APPLIED ? 1 : 0;
    }

    private Optional<NpcService> npcService(CommandSourceStack source) {
        EchoNpcServiceLookupResult lookup = EchoNpcServiceBridge.lookup(source.getServer());
        if (lookup.status() != EchoNpcServiceLookupResult.Status.RESOLVED) {
            source.sendFailure(Component.literal("Echo NPC service unavailable: " + lookup.message()));
            return Optional.empty();
        }
        return lookup.service();
    }

    private Optional<AnimationService> animationService(CommandSourceStack source) {
        AnimationServiceLookupResult lookup = AnimationServiceBridge.lookup(source.getServer());
        if (lookup.status() != AnimationServiceLookupResult.Status.RESOLVED) {
            source.sendFailure(Component.literal("Echo Animation service unavailable: " + lookup.message()));
            return Optional.empty();
        }
        return lookup.service();
    }

    private Optional<NpcActorView> actor(CommandSourceStack source, NpcService service, ActorId actorId) {
        NpcResults.Query query = service.query(actorId);
        if (query.status() == NpcResults.QueryStatus.FOUND && query.actor().isPresent()) {
            return query.actor();
        }
        source.sendFailure(Component.literal("NPC actor not found: " + actorId + " (" + query.message() + ")"));
        return Optional.empty();
    }

    private AnimationId animationId(CommandContext<CommandSourceStack> context) {
        return AnimationId.parse(ResourceLocationArgument.getId(context, "animation_id").toString());
    }

    private ActorId actorId(CommandContext<CommandSourceStack> context, String argument) {
        return new ActorId(ResourceLocationArgument.getId(context, argument).toString());
    }

    private AnimationCommandId commandId(String operation) {
        return AnimationCommandId.parse("adapter:" + operation + "_" + UUID.randomUUID().toString().replace("-", ""));
    }
}
