package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.observation;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.IntrinsicCallReader;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.NodeSpec;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.SourceNodeFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ObservationActionIntrinsics {
    private static final Map<String, String> TYPES = Map.of(
            "Observation.Activate", "echo_observation:activate",
            "Observation.Deactivate", "echo_observation:deactivate",
            "Observation.Enable", "echo_observation:enable",
            "Observation.Disable", "echo_observation:disable",
            "Observation.IsCompleted", "echo_observation:is_completed",
            "Observation.IsActive", "echo_observation:is_active");
    public static final Set<String> TARGETS = TYPES.keySet();
    private final SourceNodeFactory nodeFactory;

    public ObservationActionIntrinsics(SourceNodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    public Optional<NodeSpec> linear(IntrinsicCallReader reader, SourceSpan span) {
        String intrinsic = reader.targetName();
        reader.requirePositionalCount(intrinsic, 1);
        reader.rejectUnknownNamed(intrinsic, Set.of("player", "output"));
        Map<String, String> arguments = arguments(reader, intrinsic, 0);
        Optional<String> output = output(reader, intrinsic);
        output.ifPresent(value -> arguments.put("outputKey", value));
        return Optional.of(NodeSpec.action(span, new NodeSpec.ActionFields(Optional.empty(),
                Optional.of(TYPES.get(intrinsic)), output, arguments)));
    }

    public Optional<SourceNode> node(IntrinsicCallReader reader, SourceSpan span) {
        String intrinsic = reader.targetName();
        reader.requirePositionalCount(intrinsic, Set.of(1, 2));
        reader.rejectUnknownNamed(intrinsic, Set.of("target", "player", "output", "next"));
        Map<String, String> arguments = reader.positionalCount() == 2
                ? arguments(reader, intrinsic, 1) : namedArguments(reader, intrinsic);
        Optional<String> output = output(reader, intrinsic);
        output.ifPresent(value -> arguments.put("outputKey", value));
        return nodeFactory.node(reader, NodeSpec.action(span, new NodeSpec.ActionFields(
                reader.requiredNamedString("next", intrinsic), Optional.of(TYPES.get(intrinsic)),
                output, arguments)));
    }

    public String id(IntrinsicCallReader reader) {
        return switch (reader.targetName()) {
            case "Observation.Activate" -> "activate_observation";
            case "Observation.Deactivate" -> "deactivate_observation";
            case "Observation.Enable" -> "enable_observation";
            case "Observation.Disable" -> "disable_observation";
            case "Observation.IsCompleted" -> "is_observation_completed";
            default -> "is_observation_active";
        };
    }

    private Map<String, String> arguments(IntrinsicCallReader reader, String intrinsic, int position) {
        Map<String, String> values = new LinkedHashMap<>();
        reader.positionalScalar(position, intrinsic, "target").ifPresent(value -> values.put("target", value));
        reader.optionalNamedScalar("player", intrinsic).ifPresent(value -> values.put("player", value));
        return values;
    }

    private Map<String, String> namedArguments(IntrinsicCallReader reader, String intrinsic) {
        Map<String, String> values = new LinkedHashMap<>();
        reader.requiredNamedScalar("target", intrinsic).ifPresent(value -> values.put("target", value));
        reader.optionalNamedScalar("player", intrinsic).ifPresent(value -> values.put("player", value));
        return values;
    }

    private Optional<String> output(IntrinsicCallReader reader, String intrinsic) {
        Optional<String> explicit = reader.optionalNamedString("output", intrinsic);
        if (explicit.isPresent()) { return explicit; }
        return switch (intrinsic) {
            case "Observation.IsCompleted" -> Optional.of("observation.isCompleted");
            case "Observation.IsActive" -> Optional.of("observation.isActive");
            default -> Optional.empty();
        };
    }
}
