package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.character;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.ExternalActionIntrinsics;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.IntrinsicCallReader;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.NodeSpec;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.SourceNodeFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class NpcActionIntrinsics {
    private static final String OVERWORLD = "minecraft:overworld";
    private final SourceNodeFactory nodeFactory;
    private final NpcMovementArguments movement = new NpcMovementArguments();

    public NpcActionIntrinsics(SourceNodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    public Optional<SourceNode> nodeMove(IntrinsicCallReader reader, SourceSpan span) {
        return node(reader, span, new NodeAction("Npc.Move", "echo_navigation:npc_move",
                movement.read(reader, "Npc.Move", 1, "WALK")));
    }

    public Optional<SourceNode> nodeSprint(IntrinsicCallReader reader, SourceSpan span) {
        return node(reader, span, new NodeAction("Npc.Sprint", "echo_navigation:npc_move",
                movement.read(reader, "Npc.Sprint", 1, "SPRINT")));
    }

    public Optional<SourceNode> nodeStop(IntrinsicCallReader reader, SourceSpan span) {
        return node(reader, span, new NodeAction(
                "Npc.Stop", "echo_navigation:npc_stop", stopArgs(reader, "Npc.Stop", 1)));
    }

    public Optional<SourceNode> nodeInteract(IntrinsicCallReader reader, SourceSpan span) {
        return node(reader, span, new NodeAction("Npc.Interact", "echo_npc_interaction:interact",
                interactArgs(reader, "Npc.Interact", 1)));
    }

    public Optional<SourceNode> nodeStopInteraction(IntrinsicCallReader reader, SourceSpan span) {
        return node(reader, span, new NodeAction("Npc.StopInteraction", "echo_npc_interaction:stop",
                stopArgs(reader, "Npc.StopInteraction", 1)));
    }

    public Optional<SourceNode> nodeStandUp(IntrinsicCallReader reader, SourceSpan span) {
        return node(reader, span, new NodeAction("Npc.StandUp", "echo_npc_interaction:stand_up",
                stopArgs(reader, "Npc.StandUp", 1)));
    }

    public Optional<NodeSpec> linearInteract(IntrinsicCallReader reader, SourceSpan span) {
        acceptInteract(reader, "Npc.Interact", 0);
        return actionSpec(span, "echo_npc_interaction:interact", interactArgs(reader, "Npc.Interact", 0));
    }

    public Optional<NodeSpec> linearMove(IntrinsicCallReader reader, SourceSpan span) {
        return actionSpec(span, "echo_navigation:npc_move", movement.read(reader, "Npc.Move", 0, "WALK"));
    }

    public Optional<NodeSpec> linearSprint(IntrinsicCallReader reader, SourceSpan span) {
        return actionSpec(span, "echo_navigation:npc_move", movement.read(reader, "Npc.Sprint", 0, "SPRINT"));
    }

    public Optional<LinearNpcAction> linearCommand(
            IntrinsicCallReader reader, SourceSpan span, ExternalActionIntrinsics external) {
        return switch (reader.targetName()) {
            case "Npc.SpawnAt" -> action("spawn_npc", external.linearNpcSpawnAt(reader, span));
            case "Npc.Move" -> action("move_npc", linearMove(reader, span));
            case "Npc.Sprint" -> action("sprint_npc", linearSprint(reader, span));
            case "Npc.SleepAt" -> action("sleep_npc", external.linearNpcSleepAt(reader, span));
            case "Npc.WakeUp" -> action("wake_npc", external.linearNpcWakeUp(reader, span));
            case "Npc.Interact" -> action("interact_npc", linearInteract(reader, span));
            case "Npc.StopInteraction" -> action("stop_npc_interaction", linearStopInteraction(reader, span));
            case "Npc.StandUp" -> action("stand_up_npc", linearStandUp(reader, span));
            default -> Optional.empty();
        };
    }

    private Optional<LinearNpcAction> action(String id, Optional<NodeSpec> spec) {
        return Optional.of(new LinearNpcAction(id, spec));
    }

    public Optional<NodeSpec> linearStopInteraction(IntrinsicCallReader reader, SourceSpan span) {
        reader.requirePositionalCount("Npc.StopInteraction", 1);
        reader.rejectUnknownNamed("Npc.StopInteraction", Set.of("npc"));
        return actionSpec(span, "echo_npc_interaction:stop", stopArgs(reader, "Npc.StopInteraction", 0));
    }

    public Optional<NodeSpec> linearStandUp(IntrinsicCallReader reader, SourceSpan span) {
        reader.requirePositionalCount("Npc.StandUp", 1);
        reader.rejectUnknownNamed("Npc.StandUp", Set.of("npc"));
        return actionSpec(span, "echo_npc_interaction:stand_up", stopArgs(reader, "Npc.StandUp", 0));
    }

    private Optional<SourceNode> node(IntrinsicCallReader reader, SourceSpan span, NodeAction action) {
        return nodeFactory.node(reader, NodeSpec.action(span, new NodeSpec.ActionFields(
                reader.requiredNamedString("next", action.intrinsic()), Optional.of(action.type()),
                reader.optionalNamedString("output", action.intrinsic()), action.arguments())));
    }

    private Map<String, String> interactArgs(IntrinsicCallReader reader, String intrinsic, int offset) {
        acceptInteract(reader, intrinsic, offset);
        Map<String, String> values = xyzArgs(reader, intrinsic, offset);
        put(values, "face", reader.optionalNamedScalar("face", intrinsic).or(() -> Optional.of("AUTO")));
        put(values, "hand", reader.optionalNamedScalar("hand", intrinsic).or(() -> Optional.of("MAIN_HAND")));
        put(values, "approach", reader.optionalNamedScalar("approach", intrinsic).or(() -> Optional.of("AUTO")));
        return values;
    }

    private void acceptInteract(IntrinsicCallReader reader, String intrinsic, int offset) {
        requireCount(reader, intrinsic, offset, 4);
        reader.rejectUnknownNamed(intrinsic, Set.of("npc", "dimension", "x", "y", "z", "face",
                "hand", "approach", "next", "output"));
    }

    private Map<String, String> xyzArgs(IntrinsicCallReader reader, String intrinsic, int offset) {
        Map<String, String> values = new LinkedHashMap<>();
        if (offset == 1 && reader.positionalCount() == 1) {
            put(values, "npc", reader.requiredNamedScalar("npc", intrinsic));
            put(values, "x", reader.requiredNamedScalar("x", intrinsic));
            put(values, "y", reader.requiredNamedScalar("y", intrinsic));
            put(values, "z", reader.requiredNamedScalar("z", intrinsic));
        } else {
            put(values, "npc", reader.positionalScalar(offset, intrinsic, "npc"));
            put(values, "x", reader.positionalScalar(offset + 1, intrinsic, "x"));
            put(values, "y", reader.positionalScalar(offset + 2, intrinsic, "y"));
            put(values, "z", reader.positionalScalar(offset + 3, intrinsic, "z"));
        }
        put(values, "dimension", reader.optionalNamedScalar("dimension", intrinsic).or(() -> Optional.of(OVERWORLD)));
        return values;
    }

    private Map<String, String> stopArgs(IntrinsicCallReader reader, String intrinsic, int offset) {
        requireCount(reader, intrinsic, offset, 1);
        reader.rejectUnknownNamed(intrinsic, Set.of("npc", "next", "output"));
        Map<String, String> values = new LinkedHashMap<>();
        if (offset == 1 && reader.positionalCount() == 1) {
            put(values, "npc", reader.requiredNamedScalar("npc", intrinsic));
        } else {
            put(values, "npc", reader.positionalScalar(offset, intrinsic, "npc"));
        }
        return values;
    }

    private void requireCount(IntrinsicCallReader reader, String intrinsic, int offset, int linear) {
        if (offset == 0) {
            reader.requirePositionalCount(intrinsic, linear);
        } else {
            reader.requirePositionalCount(intrinsic, Set.of(1, linear + 1));
        }
    }

    private Optional<NodeSpec> actionSpec(SourceSpan span, String type, Map<String, String> arguments) {
        return Optional.of(NodeSpec.action(
                span, new NodeSpec.ActionFields(Optional.empty(), Optional.of(type), Optional.empty(), arguments)));
    }

    private void put(Map<String, String> values, String key, Optional<String> value) {
        value.ifPresent(present -> values.put(key, present));
    }

    private record NodeAction(String intrinsic, String type, Map<String, String> arguments) { }
    public record LinearNpcAction(String id, Optional<NodeSpec> spec) { }
}
