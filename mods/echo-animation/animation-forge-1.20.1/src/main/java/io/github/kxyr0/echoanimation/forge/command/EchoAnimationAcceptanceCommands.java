package io.github.kxyr0.echoanimation.forge.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationGraphId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationChannelDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationGraphDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.ChannelCompositionPolicy;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncRevision;
import io.github.kxyr0.echoanimation.core.runtime.AnimationSubjectRuntime;
import io.github.kxyr0.echoanimation.core.sync.AnimationSubjectSyncEncoding;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogState;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;

/**
 * Manual A2 acceptance controls for neutral animation subjects.
 */
final class EchoAnimationAcceptanceCommands {
    private static final AnimationRigId HUMANOID_RIG = AnimationRigId.parse("jdh:humanoid");
    private static final AnimationChannelId BODY = new AnimationChannelId("body");
    private static final AnimationStateId IDLE_STATE = new AnimationStateId("body/idle");
    private final AnimationCatalogState catalogState;
    private final AnimationSubjectSyncEncoding encoding = new AnimationSubjectSyncEncoding();
    private final Map<AnimationSubjectId, AnimationSyncRevision> revisions = new HashMap<>();

    EchoAnimationAcceptanceCommands(AnimationCatalogState catalogState) {
        this.catalogState = java.util.Objects.requireNonNull(catalogState, "catalogState");
    }

    LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("acceptance")
                .then(Commands.literal("state")
                        .then(Commands.argument("subjectId", ResourceLocationArgument.id())
                                .then(Commands.argument("state", StringArgumentType.word())
                                        .executes(context -> sendState(context.getSource(),
                                                subjectId(context),
                                                StringArgumentType.getString(context, "state"))))))
                .then(new EchoAnimationFaceAcceptanceCommands(catalogState).node())
                .then(Commands.literal("remove")
                        .then(Commands.argument("subjectId", ResourceLocationArgument.id())
                                .executes(context -> remove(context.getSource(), subjectId(context)))));
    }

    private AnimationSubjectId subjectId(CommandContext<CommandSourceStack> context) {
        return new AnimationSubjectId(ResourceLocationArgument.getId(context, "subjectId").toString());
    }

    private int sendState(CommandSourceStack source, AnimationSubjectId subjectId, String rawState) {
        Optional<EchoAnimationAcceptanceState> requested = EchoAnimationAcceptanceState.from(rawState);
        if (requested.isEmpty()) {
            source.sendFailure(Component.literal("Unknown A2 acceptance state: " + rawState
                    + ". Available: " + EchoAnimationAcceptanceState.availableNames()));
            return 0;
        }
        EchoAnimationAcceptanceState state = requested.orElseThrow();
        if (!catalogReady(source, state)) {
            return 0;
        }
        AnimationGraphDefinition graph = graph();
        AnimationSubjectRuntime runtime = new AnimationSubjectRuntime(subjectId, graph);
        state.enter(runtime);
        AnimationSyncRevision revision = nextRevision(subjectId);
        var snapshot = encoding.encode(graph, runtime.snapshot(), revision, source.getLevel().getGameTime());
        int sent = source.getServer().getPlayerList().getPlayers().stream()
                .mapToInt(player -> catalogState.service().sendFullSnapshot(player, snapshot) ? 1 : 0)
                .sum();
        source.sendSuccess(() -> Component.literal("Echo Animation acceptance subject " + subjectId
                + " -> " + state.wireName() + ", clients=" + sent), false);
        return sent;
    }

    private int remove(CommandSourceStack source, AnimationSubjectId subjectId) {
        AnimationSyncRevision revision = nextRevision(subjectId);
        int sent = source.getServer().getPlayerList().getPlayers().stream()
                .mapToInt(player -> catalogState.service().sendSubjectRemoval(player, subjectId, revision) ? 1 : 0)
                .sum();
        source.sendSuccess(() -> Component.literal("Echo Animation acceptance subject removed "
                + subjectId + ", clients=" + sent), false);
        return sent;
    }

    private boolean catalogReady(CommandSourceStack source, EchoAnimationAcceptanceState state) {
        if (catalogState.current().animationRigs().stream().noneMatch(rig -> rig.id().equals(HUMANOID_RIG))) {
            source.sendFailure(Component.literal("A2 acceptance rig missing: " + HUMANOID_RIG));
            return false;
        }
        boolean found = catalogState.current().animationClips().stream()
                .anyMatch(clip -> clip.id().equals(state.clipId()));
        if (!found) {
            source.sendFailure(Component.literal("A2 acceptance clip missing: " + state.clipId()));
        }
        return found;
    }

    private AnimationSyncRevision nextRevision(AnimationSubjectId subjectId) {
        AnimationSyncRevision previous = revisions.get(subjectId);
        AnimationSyncRevision next = previous == null ? new AnimationSyncRevision(1L) : previous.next();
        revisions.put(subjectId, next);
        return next;
    }

    private AnimationGraphDefinition graph() {
        return new AnimationGraphDefinition(new AnimationGraphId("jdh:a2_acceptance"), 1L,
                List.of(new AnimationChannelDefinition(BODY, IDLE_STATE, 0, ChannelCompositionPolicy.OVERRIDE,
                        Set.of())),
                EchoAnimationAcceptanceState.states(),
                EchoAnimationAcceptanceState.transitions(BODY, IDLE_STATE, 5L));
    }
}
