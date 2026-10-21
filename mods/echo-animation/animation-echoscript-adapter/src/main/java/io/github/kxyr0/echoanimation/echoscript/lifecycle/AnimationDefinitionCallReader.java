package io.github.kxyr0.echoanimation.echoscript.lifecycle;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.animation.marker.AnimationMarkerDefinition;
import io.github.kxyr0.echoanimation.api.animation.marker.AnimationMarkerId;
import io.github.kxyr0.echoanimation.api.animation.definition.MainMode;
import io.github.kxyr0.echoanimation.api.animation.definition.RecoveryPolicy;
import io.github.kxyr0.echoanimation.api.animation.definition.ReplayPolicy;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.definition.fingerprint.SemanticFingerprinter;
import io.github.kxyr0.echoanimation.echoscript.AnimationCompileResult;
import io.github.kxyr0.echoanimation.echoscript.binding.ArgumentReader;
import io.github.kxyr0.echoanimation.echoscript.syntax.CallSyntax;
import io.github.kxyr0.echoanimation.echoscript.diagnostic.AnimationSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reads high-level Animation(...) lifecycle resources.
 */
public final class AnimationDefinitionCallReader {
    private final ArgumentReader arguments = new ArgumentReader();

    public AnimationCompileResult read(InvocationExpressionSyntax call, List<AnimationDiagnostic> diagnostics) {
        try {
            arguments.requireKnown(call, List.of("channels", "sources", "entries", "main",
                    "exits", "markers", "target", "recovery", "replay"), diagnostics);
            AnimationDefinition definition = definition(call);
            return new AnimationCompileResult(Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.of(definition), Optional.empty(), diagnostics);
        } catch (RuntimeException exception) {
            diagnostics.add(AnimationSemanticDiagnostics.error("EANIM-ES-ANIMATION",
                    exception.getMessage(), call.span()));
            return new AnimationCompileResult(Optional.empty(), Optional.empty(), Optional.empty(), diagnostics);
        }
    }

    private AnimationDefinition definition(InvocationExpressionSyntax call) {
        AnimationId id = AnimationId.parse(arguments.string(call, 0, null).orElseThrow());
        Set<AnimationChannelId> channels = channels(call);
        Set<AnimationStateId> sources = states(call, "sources", "Source");
        List<AnimationDefinition.EntryVariant> entries = arguments.calls(call, "entries").stream()
                .map(this::entry).toList();
        AnimationDefinition.MainPhase main = main(arguments.call(call, "main").orElseThrow());
        List<AnimationDefinition.ExitVariant> exits = arguments.calls(call, "exits").stream()
                .map(this::exit).toList();
        List<AnimationMarkerDefinition> markers = arguments.calls(call, "markers").stream()
                .map(this::marker).toList();
        AnimationStateId target = new AnimationStateId(arguments.string(call, -1, "target").orElseThrow());
        Recovery recovery = recovery(arguments.call(call, "recovery").orElseThrow());
        ReplayPolicy replay = ReplayPolicy.valueOf(arguments.string(call, -1, "replay")
                .orElse(ReplayPolicy.REJECT_IF_RUNNING.name()));
        return new AnimationDefinition(id, channels, sources, entries, main, exits, markers, target,
                recovery.policy(), recovery.checkpointIntervalTicks(), replay,
                io.github.kxyr0.echoanimation.api.animation.definition.AnimationBlendPolicy.NONE,
                fingerprint(call));
    }

    private Set<AnimationChannelId> channels(InvocationExpressionSyntax call) {
        Set<AnimationChannelId> result = new LinkedHashSet<>();
        for (InvocationExpressionSyntax item : arguments.calls(call, "channels")) {
            require(item, "Channel");
            result.add(new AnimationChannelId(arguments.string(item, 0, null).orElseThrow()));
        }
        return result;
    }

    private Set<AnimationStateId> states(InvocationExpressionSyntax call, String field, String intrinsic) {
        Set<AnimationStateId> result = new LinkedHashSet<>();
        for (InvocationExpressionSyntax item : arguments.calls(call, field)) {
            require(item, intrinsic);
            result.add(new AnimationStateId(arguments.string(item, 0, null).orElseThrow()));
        }
        return result;
    }

    private AnimationDefinition.EntryVariant entry(InvocationExpressionSyntax call) {
        require(call, "Entry");
        return new AnimationDefinition.EntryVariant(
                new AnimationStateId(arguments.string(call, 0, null).orElseThrow()),
                AnimationClipId.parse(arguments.string(call, -1, "clip").orElseThrow()),
                arguments.integer(call, -1, "durationTicks", 0));
    }

    private AnimationDefinition.MainPhase main(InvocationExpressionSyntax call) {
        require(call, "Main");
        return new AnimationDefinition.MainPhase(
                AnimationClipId.parse(arguments.string(call, -1, "clip").orElseThrow()),
                MainMode.valueOf(arguments.string(call, -1, "mode").orElseThrow()),
                arguments.integer(call, -1, "durationTicks", 1),
                arguments.calls(call, "exitPoints").stream().map(this::exitPoint).toList());
    }

    private AnimationDefinition.ExitPoint exitPoint(InvocationExpressionSyntax call) {
        require(call, "ExitPoint");
        return new AnimationDefinition.ExitPoint(arguments.string(call, 0, null).orElseThrow(),
                arguments.authoredNumber(call, -1, "progress", 0));
    }

    private AnimationDefinition.ExitVariant exit(InvocationExpressionSyntax call) {
        require(call, "Exit");
        return new AnimationDefinition.ExitVariant(arguments.string(call, 0, null).orElseThrow(),
                AnimationClipId.parse(arguments.string(call, -1, "clip").orElseThrow()),
                arguments.integer(call, -1, "durationTicks", 0));
    }

    private AnimationMarkerDefinition marker(InvocationExpressionSyntax call) {
        require(call, "Marker");
        return new AnimationMarkerDefinition(
                AnimationMarkerId.parse(arguments.string(call, 0, null).orElseThrow()),
                AnimationPhase.valueOf(arguments.string(call, -1, "phase").orElseThrow()),
                arguments.integer(call, -1, "tick", 1));
    }

    private Recovery recovery(InvocationExpressionSyntax call) {
        require(call, "Recovery");
        return new Recovery(RecoveryPolicy.valueOf(arguments.string(call, 0, null).orElseThrow()),
                arguments.integer(call, -1, "checkpointIntervalTicks", 0));
    }

    private String fingerprint(InvocationExpressionSyntax call) {
        return new SemanticFingerprinter().fingerprint(List.of(call.toString()));
    }

    private void require(InvocationExpressionSyntax call, String name) {
        if (!CallSyntax.named(call, name)) {
            throw new IllegalArgumentException("Expected intrinsic " + name + ".");
        }
    }

    private record Recovery(RecoveryPolicy policy, int checkpointIntervalTicks) {
    }
}
