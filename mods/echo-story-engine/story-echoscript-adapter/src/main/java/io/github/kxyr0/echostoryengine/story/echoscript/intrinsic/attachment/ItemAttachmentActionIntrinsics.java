package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.attachment;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.IntrinsicCallReader;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.NodeSpec;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.SourceNodeFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ItemAttachmentActionIntrinsics {
    public static final Set<String> TARGETS = Set.of(
            "Npc.TakeItem", "Npc.MoveAttachedItem", "Npc.DropItem", "Npc.ThrowItem",
            "Player.TakeItem", "Player.MoveAttachedItem", "Player.DropItem", "Player.ThrowItem");
    private static final String OVERWORLD = "minecraft:overworld";
    private final SourceNodeFactory nodeFactory;

    public ItemAttachmentActionIntrinsics(SourceNodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    public Optional<NodeSpec> linear(IntrinsicCallReader reader, SourceSpan span) {
        return Optional.of(spec(reader, span, 0));
    }

    public Optional<SourceNode> node(IntrinsicCallReader reader, SourceSpan span) {
        NodeSpec spec = spec(reader, span, 1);
        return nodeFactory.node(reader, NodeSpec.action(span, new NodeSpec.ActionFields(
                reader.requiredNamedString("next", reader.targetName()), spec.actionType(),
                spec.output(), spec.arguments())));
    }

    public String id(IntrinsicCallReader reader) {
        String operation = operation(reader.targetName());
        return (reader.targetName().startsWith("Player.") ? "player_" : "npc_") + operation + "_item";
    }

    private NodeSpec spec(IntrinsicCallReader reader, SourceSpan span, int offset) {
        String target = reader.targetName();
        boolean player = target.startsWith("Player.");
        String operation = operation(target);
        int subjectCount = player ? 0 : 1;
        int positionCount = operation.equals("take") ? 1 : 0;
        reader.requirePositionalCount(target, offset + subjectCount + positionCount);
        reader.rejectUnknownNamed(target, Set.of("hand", "from", "to", "expected", "count",
                "animation", "release", "dimension", "radius", "speed", "next", "output"));
        Map<String, String> values = new LinkedHashMap<>();
        values.put("subjectKind", player ? "PLAYER" : "NPC");
        if (player) {
            values.put("player", "player");
        } else {
            put(values, "npc", reader.positionalReference(offset, target, "npc"));
        }
        if (operation.equals("take")) {
            int positionIndex = offset + subjectCount;
            put(values, "position", reader.positionalReferenceOrVector(positionIndex, target, "position"));
            put(values, "hand", reader.requiredNamedReference("hand", target));
            put(values, "expected", reader.optionalNamedScalar("expected", target));
            put(values, "count", reader.optionalNamedScalar("count", target).or(() -> Optional.of("1")));
            put(values, "radius", reader.optionalNamedScalar("radius", target).or(() -> Optional.of("0.75")));
            put(values, "dimension", reader.optionalNamedScalar("dimension", target)
                    .or(() -> Optional.of(OVERWORLD)));
        } else if (operation.equals("move")) {
            put(values, "from", reader.requiredNamedReference("from", target));
            put(values, "to", reader.requiredNamedReference("to", target));
        } else {
            put(values, "hand", reader.requiredNamedReference("hand", target));
            put(values, "release", reader.optionalNamedReference("release", target)
                    .or(() -> Optional.of(operation.equals("throw") ? "HAND_MOTION" : "LOOK_DIRECTION")));
            put(values, "speed", reader.optionalNamedScalar("speed", target)
                    .or(() -> Optional.of(operation.equals("throw") ? "1.0" : "0.15")));
        }
        put(values, "animation", reader.optionalNamedReference("animation", target));
        return NodeSpec.action(span, new NodeSpec.ActionFields(Optional.empty(),
                Optional.of("echo_item_attachments:" + operation), Optional.empty(), Map.copyOf(values)));
    }

    private String operation(String target) {
        if (target.endsWith("TakeItem")) {
            return "take";
        }
        if (target.endsWith("MoveAttachedItem")) {
            return "move";
        }
        if (target.endsWith("DropItem")) {
            return "drop";
        }
        return "throw";
    }

    private void put(Map<String, String> values, String key, Optional<String> value) {
        value.ifPresent(present -> values.put(key, present));
    }
}
