package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.actions;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.IntrinsicCallReader;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.NodeSpec;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.SourceNodeFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class BlackspaceActionIntrinsics {
    private static final String CREATE = "echo_blackspace:create";
    private static final String TELEPORT_PLAYER = "echo_blackspace:teleport_player";

    private final SourceNodeFactory nodeFactory;

    public BlackspaceActionIntrinsics(SourceNodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    public Optional<NodeSpec> linearCreate(
            IntrinsicCallReader reader,
            SourceSpan span,
            Optional<String> outputReference) {
        accept(reader, "Blackspace.Create", 1, Set.of("playerName", "playerUuid"));
        Map<String, String> arguments = positionalArgs(reader, "Blackspace.Create", "instanceId");
        putPlayerSelector(arguments, reader, "Blackspace.Create");
        outputReference.ifPresent(reference -> arguments.put("spawnOutput", reference));
        return Optional.of(NodeSpec.action(span, new NodeSpec.ActionFields(
                Optional.empty(), Optional.of(CREATE), outputReference, arguments)));
    }

    public Optional<NodeSpec> linearTeleport(
            IntrinsicCallReader reader,
            SourceSpan span,
            Map<String, String> blackspaceVariables) {
        accept(reader, "Player.Teleport", 1, Set.of("playerName", "playerUuid"));
        Map<String, String> arguments = new LinkedHashMap<>();
        reader.positionalReference(0, "Player.Teleport", "target")
                .ifPresent(target -> putTeleportTarget(arguments, target, blackspaceVariables));
        putPlayerSelector(arguments, reader, "Player.Teleport");
        return Optional.of(NodeSpec.action(span, new NodeSpec.ActionFields(
                Optional.empty(), Optional.of(TELEPORT_PLAYER), Optional.empty(), arguments)));
    }

    public Optional<SourceNode> createNode(IntrinsicCallReader reader, SourceSpan span) {
        reader.requirePositionalCount("Blackspace.Create", Set.of(1, 2));
        reader.rejectUnknownNamed("Blackspace.Create", Set.of("instanceId", "id", "playerName", "playerUuid",
                "next", "output"));
        Map<String, String> arguments = new LinkedHashMap<>();
        if (reader.positionalCount() == 2) {
            put(arguments, "instanceId", reader.positionalScalar(1, "Blackspace.Create", "instanceId"));
        } else {
            putRequiredInstanceId(arguments, reader);
        }
        putPlayerSelector(arguments, reader, "Blackspace.Create");
        Optional<String> output = reader.optionalNamedString("output", "Blackspace.Create");
        output.ifPresent(value -> arguments.put("spawnOutput", value));
        return nodeFactory.node(reader, NodeSpec.action(span, new NodeSpec.ActionFields(
                reader.requiredNamedString("next", "Blackspace.Create"), Optional.of(CREATE), output, arguments)));
    }

    public Optional<SourceNode> teleportNode(IntrinsicCallReader reader, SourceSpan span) {
        reader.requirePositionalCount("Player.Teleport", Set.of(1, 2));
        reader.rejectUnknownNamed("Player.Teleport", Set.of("instanceId", "target", "playerName", "playerUuid",
                "next", "output"));
        Map<String, String> arguments = new LinkedHashMap<>();
        if (reader.positionalCount() == 2) {
            reader.positionalReference(1, "Player.Teleport", "target")
                    .ifPresent(target -> putTeleportTarget(arguments, target, Map.of()));
        } else {
            putNamedTeleportTarget(arguments, reader);
        }
        putPlayerSelector(arguments, reader, "Player.Teleport");
        return nodeFactory.node(reader, NodeSpec.action(span, new NodeSpec.ActionFields(
                reader.requiredNamedString("next", "Player.Teleport"), Optional.of(TELEPORT_PLAYER),
                reader.optionalNamedString("output", "Player.Teleport"), arguments)));
    }

    public Optional<String> linearInstanceId(IntrinsicCallReader reader) {
        return reader.positionalString(0, "Blackspace.Create", "instanceId");
    }

    private Map<String, String> positionalArgs(IntrinsicCallReader reader, String intrinsic, String... names) {
        Map<String, String> arguments = new LinkedHashMap<>();
        for (int index = 0; index < names.length; index++) {
            put(arguments, names[index], reader.positionalScalar(index, intrinsic, names[index]));
        }
        return arguments;
    }

    private void putRequiredInstanceId(Map<String, String> arguments, IntrinsicCallReader reader) {
        Optional<String> instanceId = reader.optionalNamedScalar("instanceId", "Blackspace.Create")
                .or(() -> reader.optionalNamedScalar("id", "Blackspace.Create"));
        put(arguments, "instanceId", instanceId.isPresent()
                ? instanceId
                : reader.requiredNamedScalar("instanceId", "Blackspace.Create"));
    }

    private void putNamedTeleportTarget(Map<String, String> arguments, IntrinsicCallReader reader) {
        Optional<String> instanceId = reader.optionalNamedScalar("instanceId", "Player.Teleport");
        Optional<String> target = reader.optionalNamedScalar("target", "Player.Teleport");
        if (instanceId.isPresent()) {
            put(arguments, "instanceId", instanceId);
        } else if (target.isPresent()) {
            put(arguments, "target", target);
        } else {
            put(arguments, "instanceId", reader.requiredNamedScalar("instanceId", "Player.Teleport"));
        }
    }

    private void putPlayerSelector(Map<String, String> arguments, IntrinsicCallReader reader, String intrinsic) {
        put(arguments, "playerName", reader.optionalNamedScalar("playerName", intrinsic));
        put(arguments, "playerUuid", reader.optionalNamedScalar("playerUuid", intrinsic));
    }

    private void putTeleportTarget(
            Map<String, String> arguments,
            String target,
            Map<String, String> blackspaceVariables) {
        if (target.endsWith(".Spawn")) {
            String variable = target.substring(0, target.length() - ".Spawn".length());
            String instanceId = blackspaceVariables.get(variable);
            if (instanceId != null) {
                arguments.put("instanceId", instanceId);
                return;
            }
        }
        if (target.indexOf(':') > 0 && !target.contains(";")) {
            arguments.put("instanceId", target);
        } else {
            arguments.put("target", target);
        }
    }

    private void accept(IntrinsicCallReader reader, String intrinsic, int positionalCount, Set<String> named) {
        reader.requirePositionalCount(intrinsic, positionalCount);
        reader.rejectUnknownNamed(intrinsic, named);
    }

    private void put(Map<String, String> map, String key, Optional<String> value) {
        value.ifPresent(present -> map.put(key, present));
    }
}
