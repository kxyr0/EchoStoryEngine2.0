package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.features;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.IntrinsicCallReader;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.NodeSpec;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Frontend shorthand only; the external adapter owns feature state and gameplay. */
public final class FeatureActionIntrinsics {
    public static final Set<String> TARGETS = Set.of("Feature.Unlock", "Feature.Lock", "Feature.Set");
    private static final Map<String, String> ALIASES = Map.of(
            "canvas", "thought_space_unlocked", "magic", "magic_unlocked");

    public Optional<NodeSpec> linear(IntrinsicCallReader reader, SourceSpan span) {
        String intrinsic = reader.targetName();
        reader.requirePositionalCount(intrinsic, intrinsic.equals("Feature.Set") ? 2 : 1);
        reader.rejectUnknownNamed(intrinsic, Set.of("player"));
        Map<String, String> arguments = new LinkedHashMap<>();
        reader.positionalString(0, intrinsic, "flag").ifPresent(flag -> {
            String canonical = ALIASES.getOrDefault(flag, flag);
            if (!canonical.matches("[a-z][a-z0-9_]{0,63}")) {
                reader.report("ESE-SOURCE-024", Map.of("intrinsic", intrinsic, "argument", "flag",
                        "expected", "lowercase feature flag"));
            }
            arguments.put("flag", canonical);
        });
        String value = switch (intrinsic) {
            case "Feature.Unlock" -> "true";
            case "Feature.Lock" -> "false";
            default -> reader.positionalScalar(1, intrinsic, "value").orElse("");
        };
        if (!value.equals("true") && !value.equals("false")) {
            reader.report("ESE-SOURCE-024", Map.of("intrinsic", intrinsic, "argument", "value", "expected", "boolean"));
        }
        arguments.put("value", value);
        arguments.put("player", reader.optionalNamedString("player", intrinsic).orElse("@a"));
        return Optional.of(NodeSpec.action(span, new NodeSpec.ActionFields(Optional.empty(),
                Optional.of("echo_core:set_flag"), Optional.empty(), arguments)));
    }
}
