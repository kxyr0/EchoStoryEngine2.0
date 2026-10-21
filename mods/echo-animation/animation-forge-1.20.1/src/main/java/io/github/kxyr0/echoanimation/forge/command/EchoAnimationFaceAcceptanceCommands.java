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
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
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
 * Manual A5 face animation preview controls.
 */
final class EchoAnimationFaceAcceptanceCommands {
    private static final FaceRigId FACE_RIG = FaceRigId.parse("jdh:john_father");
    private static final AnimationChannelId FACE = new AnimationChannelId("face");
    private static final AnimationStateId NEUTRAL = new AnimationStateId("face/neutral");
    private final AnimationCatalogState catalogState;
    private final AnimationSubjectSyncEncoding encoding = new AnimationSubjectSyncEncoding();
    private final Map<AnimationSubjectId, AnimationSyncRevision> revisions = new HashMap<>();

    EchoAnimationFaceAcceptanceCommands(AnimationCatalogState catalogState) {
        this.catalogState = java.util.Objects.requireNonNull(catalogState, "catalogState");
    }

    LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("face")
                .then(Commands.argument("subjectId", ResourceLocationArgument.id())
                        .then(Commands.argument("state", StringArgumentType.word())
                                .executes(context -> sendState(context.getSource(), subjectId(context),
                                        StringArgumentType.getString(context, "state")))));
    }

    private AnimationSubjectId subjectId(CommandContext<CommandSourceStack> context) {
        return new AnimationSubjectId(ResourceLocationArgument.getId(context, "subjectId").toString());
    }

    private int sendState(CommandSourceStack source, AnimationSubjectId subjectId, String rawState) {
        Optional<EchoAnimationFaceAcceptanceState> requested = EchoAnimationFaceAcceptanceState.from(rawState);
        if (requested.isEmpty()) {
            source.sendFailure(Component.literal("Unknown face acceptance state: " + rawState
                    + ". Available: " + EchoAnimationFaceAcceptanceState.availableNames()));
            return 0;
        }
        EchoAnimationFaceAcceptanceState state = requested.orElseThrow();
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
        source.sendSuccess(() -> Component.literal("Echo Animation face acceptance subject " + subjectId
                + " -> " + state.wireName() + ", clients=" + sent), false);
        return sent;
    }

    private boolean catalogReady(CommandSourceStack source, EchoAnimationFaceAcceptanceState state) {
        boolean rigFound = catalogState.current().faceRigs().stream().anyMatch(rig -> rig.id().equals(FACE_RIG));
        if (!rigFound) {
            source.sendFailure(Component.literal("A5 face acceptance rig missing: " + FACE_RIG));
            return false;
        }
        boolean clipFound = catalogState.current().animationClips().stream()
                .anyMatch(clip -> clip.id().equals(state.clipId()));
        if (!clipFound) {
            source.sendFailure(Component.literal("A5 face acceptance clip missing: " + state.clipId()));
        }
        return clipFound;
    }

    private AnimationSyncRevision nextRevision(AnimationSubjectId subjectId) {
        AnimationSyncRevision previous = revisions.get(subjectId);
        AnimationSyncRevision next = previous == null ? new AnimationSyncRevision(1L) : previous.next();
        revisions.put(subjectId, next);
        return next;
    }

    private AnimationGraphDefinition graph() {
        return new AnimationGraphDefinition(new AnimationGraphId("jdh:face_acceptance"), 1L,
                List.of(new AnimationChannelDefinition(FACE, NEUTRAL, 1, ChannelCompositionPolicy.OVERRIDE,
                        Set.of())),
                EchoAnimationFaceAcceptanceState.states(),
                EchoAnimationFaceAcceptanceState.transitions(FACE, NEUTRAL));
    }
}
