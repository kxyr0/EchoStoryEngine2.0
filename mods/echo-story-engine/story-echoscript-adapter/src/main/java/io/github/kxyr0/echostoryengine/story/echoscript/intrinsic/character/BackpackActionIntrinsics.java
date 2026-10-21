package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.character;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.IntrinsicCallReader;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.NodeSpec;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.SourceNodeFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class BackpackActionIntrinsics {
    public static final Set<String> TARGETS = Set.of("Npc.EquipBackpack", "Npc.UnequipBackpack");
    private static final String OVERWORLD = "minecraft:overworld";
    private final SourceNodeFactory nodeFactory;

    public BackpackActionIntrinsics(SourceNodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    public Optional<NodeSpec> linear(IntrinsicCallReader reader, SourceSpan span) {
        return Optional.of(spec(reader, span, 0));
    }

    public Optional<SourceNode> node(IntrinsicCallReader reader, SourceSpan span) {
        String intrinsic = reader.targetName();
        NodeSpec spec = spec(reader, span, 1);
        return nodeFactory.node(reader, NodeSpec.action(span, new NodeSpec.ActionFields(
                reader.requiredNamedString("next", intrinsic), spec.actionType(), spec.output(), spec.arguments())));
    }

    public String id(IntrinsicCallReader reader) {
        return reader.targetName().equals("Npc.EquipBackpack") ? "equip_backpack" : "unequip_backpack";
    }

    private NodeSpec spec(IntrinsicCallReader reader, SourceSpan span, int offset) {
        String intrinsic = reader.targetName();
        int count = reader.positionalCount();
        reader.requirePositionalCount(intrinsic, offset == 0 ? Set.of(2, 4) : Set.of(3, 5));
        reader.rejectUnknownNamed(intrinsic, Set.of("npc", "dimension", "x", "y", "z", "position",
                "next", "output"));
        Map<String, String> arguments = count == offset + 2
                ? positionArguments(reader, intrinsic, offset) : coordinateArguments(reader, intrinsic, offset);
        String type = intrinsic.equals("Npc.EquipBackpack")
                ? "echo_npc_sophisticated_backpacks:equip" : "echo_npc_sophisticated_backpacks:unequip";
        return NodeSpec.action(span, new NodeSpec.ActionFields(Optional.empty(), Optional.of(type),
                Optional.empty(), arguments));
    }

    private Map<String, String> positionArguments(IntrinsicCallReader reader, String intrinsic, int offset) {
        Map<String, String> values = base(reader, intrinsic, offset);
        reader.positionalVector(offset + 1, intrinsic, "position").ifPresent(position -> {
            values.put("x", position.get(0));
            values.put("y", position.get(1));
            values.put("z", position.get(2));
        });
        return values;
    }

    private Map<String, String> coordinateArguments(IntrinsicCallReader reader, String intrinsic, int offset) {
        Map<String, String> values = base(reader, intrinsic, offset);
        put(values, "x", reader.positionalScalar(offset + 1, intrinsic, "x"));
        put(values, "y", reader.positionalScalar(offset + 2, intrinsic, "y"));
        put(values, "z", reader.positionalScalar(offset + 3, intrinsic, "z"));
        return values;
    }

    private Map<String, String> base(IntrinsicCallReader reader, String intrinsic, int offset) {
        Map<String, String> values = new LinkedHashMap<>();
        put(values, "npc", reader.positionalScalar(offset, intrinsic, "npc"));
        put(values, "dimension", reader.optionalNamedScalar("dimension", intrinsic)
                .or(() -> Optional.of(OVERWORLD)));
        return values;
    }

    private void put(Map<String, String> values, String key, Optional<String> value) {
        value.ifPresent(present -> values.put(key, present));
    }
}
