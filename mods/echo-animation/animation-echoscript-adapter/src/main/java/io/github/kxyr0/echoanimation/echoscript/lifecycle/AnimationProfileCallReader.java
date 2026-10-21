package io.github.kxyr0.echoanimation.echoscript.lifecycle;

import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationGraphId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.profile.AnimationProfileFingerprint;
import io.github.kxyr0.echoanimation.api.profile.AnimationProfileId;
import io.github.kxyr0.echoanimation.api.profile.definition.AnimationProfileDefinition;
import io.github.kxyr0.echoanimation.api.profile.definition.AnimationRenderModelKind;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.definition.fingerprint.SemanticFingerprinter;
import io.github.kxyr0.echoanimation.echoscript.AnimationCompileResult;
import io.github.kxyr0.echoanimation.echoscript.binding.ArgumentReader;
import io.github.kxyr0.echoanimation.echoscript.syntax.CallSyntax;
import io.github.kxyr0.echoanimation.echoscript.diagnostic.AnimationSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads AnimationProfile(...) subject provisioning resources.
 */
public final class AnimationProfileCallReader {
    private final ArgumentReader arguments = new ArgumentReader();

    public AnimationCompileResult read(InvocationExpressionSyntax call, List<AnimationDiagnostic> diagnostics) {
        try {
            arguments.requireKnown(call, List.of("rig", "graph", "initialStates",
                    "requiredChannels", "render", "metadata"), diagnostics);
            AnimationProfileDefinition profile = profile(call);
            return new AnimationCompileResult(Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(profile), diagnostics);
        } catch (RuntimeException exception) {
            diagnostics.add(AnimationSemanticDiagnostics.error("EANIM-ES-PROFILE",
                    exception.getMessage(), call.span()));
            return new AnimationCompileResult(Optional.empty(), Optional.empty(), Optional.empty(), diagnostics);
        }
    }

    private AnimationProfileDefinition profile(InvocationExpressionSyntax call) {
        AnimationProfileId id = AnimationProfileId.parse(arguments.string(call, 0, null).orElseThrow());
        AnimationRigId rig = AnimationRigId.parse(arguments.string(call, -1, "rig").orElseThrow());
        AnimationGraphId graph = new AnimationGraphId(arguments.string(call, -1, "graph").orElseThrow());
        Map<AnimationChannelId, AnimationStateId> initialStates = initialStates(call);
        Set<AnimationChannelId> required = requiredChannels(call, initialStates);
        AnimationRenderModelKind render = AnimationRenderModelKind.valueOf(arguments.string(call, -1, "render")
                .orElse(AnimationRenderModelKind.SEGMENTED_HUMANOID.name()));
        Map<String, String> metadata = metadata(call);
        String fingerprint = new SemanticFingerprinter().fingerprint(List.of(call.toString()));
        return new AnimationProfileDefinition(id, rig, graph, initialStates, required, render, metadata,
                new AnimationProfileFingerprint(fingerprint));
    }

    private Map<AnimationChannelId, AnimationStateId> initialStates(InvocationExpressionSyntax call) {
        Map<AnimationChannelId, AnimationStateId> result = new LinkedHashMap<>();
        for (InvocationExpressionSyntax item : arguments.calls(call, "initialStates")) {
            require(item, "InitialState");
            AnimationChannelId channel = new AnimationChannelId(arguments.string(item, 0, null).orElseThrow());
            AnimationStateId state = new AnimationStateId(arguments.string(item, -1, "state").orElseThrow());
            if (result.putIfAbsent(channel, state) != null) {
                throw new IllegalArgumentException("Duplicate profile initial state channel: " + channel);
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("AnimationProfile must declare at least one initial state.");
        }
        return result;
    }

    private Set<AnimationChannelId> requiredChannels(
            InvocationExpressionSyntax call,
            Map<AnimationChannelId, AnimationStateId> initialStates
    ) {
        Set<AnimationChannelId> result = new LinkedHashSet<>();
        for (InvocationExpressionSyntax item : arguments.calls(call, "requiredChannels")) {
            require(item, "Channel");
            result.add(new AnimationChannelId(arguments.string(item, 0, null).orElseThrow()));
        }
        if (result.isEmpty()) {
            result.addAll(initialStates.keySet());
        }
        return result;
    }

    private Map<String, String> metadata(InvocationExpressionSyntax call) {
        Map<String, String> result = new LinkedHashMap<>();
        for (InvocationExpressionSyntax item : arguments.calls(call, "metadata")) {
            require(item, "Meta");
            String key = arguments.string(item, 0, null).orElseThrow();
            String value = arguments.string(item, -1, "value").orElseThrow();
            result.put(key, value);
        }
        return result;
    }

    private void require(InvocationExpressionSyntax call, String name) {
        if (!CallSyntax.named(call, name)) {
            throw new IllegalArgumentException("Expected intrinsic " + name + ".");
        }
    }
}
