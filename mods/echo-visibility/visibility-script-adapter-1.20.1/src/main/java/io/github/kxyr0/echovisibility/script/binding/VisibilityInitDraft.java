package io.github.kxyr0.echovisibility.script.binding;

import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnostic;
import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnosticCode;
import io.github.kxyr0.echovisibility.api.identity.VisibilityDefinitionRevision;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.api.model.VisibilityWatchDefinition;
import io.github.kxyr0.echovisibility.api.policy.VisibilityConcealmentPolicy;
import io.github.kxyr0.echovisibility.api.policy.VisibilityDetectionMode;
import io.github.kxyr0.echovisibility.api.policy.VisibilityEvaluationRate;
import io.github.kxyr0.echovisibility.api.policy.VisibilityHysteresis;
import io.github.kxyr0.echovisibility.api.policy.VisibilityOcclusionMode;
import io.github.kxyr0.echovisibility.api.policy.VisibilityViewpoint;
import io.github.kxyr0.echovisibility.api.sample.VisibilitySampleKind;
import io.github.kxyr0.echovisibility.api.sample.VisibilitySamplePolicy;
import io.github.kxyr0.echovisibility.api.target.VisibilityBoxTarget;
import io.github.kxyr0.echovisibility.api.target.VisibilityTarget;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Optional;

final class VisibilityInitDraft {
    private final VisibilityTargetBinder targetBinder = new VisibilityTargetBinder();
    private final VisibilityPolicyBinder policies = new VisibilityPolicyBinder();
    private Optional<VisibilityWatchId> id = Optional.empty();
    private Optional<VisibilityTarget> target = Optional.empty();
    private VisibilityViewpoint viewpoint = VisibilityViewpoint.ACTIVE_CAMERA;
    private VisibilityDetectionMode detection = VisibilityDetectionMode.OUTSIDE_FRUSTUM;
    private VisibilityOcclusionMode occlusion = VisibilityOcclusionMode.NONE;
    private Optional<List<VisibilitySampleKind>> samples = Optional.empty();
    private int lostFrames = 1;
    private int seenFrames = 2;
    private boolean requireVisibleFirst = true;
    private int edgeMarginPixels = 2;
    private VisibilityConcealmentPolicy concealment = VisibilityConcealmentPolicy.NONE;
    private VisibilityEvaluationRate evaluation = VisibilityEvaluationRate.EVERY_RENDER_FRAME;

    void read(MethodSyntax init, VisibilityFieldCatalog fields, List<VisibilityDiagnostic> diagnostics) {
        for (var statement : init.body().statements()) {
            Optional<InvocationExpressionSyntax> call = VisibilityCallSyntax.statementCall(statement);
            if (call.isEmpty()) {
                invalid(diagnostics, "VisibilityWatchResource Init() supports invocation statements only.");
            } else {
                readCall(call.orElseThrow(), fields, diagnostics);
            }
        }
    }

    Optional<VisibilityWatchDefinition> build(
            VisibilityDefinitionRevision revision,
            String fingerprint,
            List<VisibilityDiagnostic> diagnostics) {
        if (id.isEmpty()) {
            invalid(diagnostics, "VisibilityWatchResource Init() must declare Id(...).");
        }
        if (target.isEmpty()) {
            invalid(diagnostics, "VisibilityWatchResource Init() must declare Target(...).");
        }
        if (!diagnostics.isEmpty() || id.isEmpty() || target.isEmpty()) {
            return Optional.empty();
        }
        VisibilitySamplePolicy samplePolicy = new VisibilitySamplePolicy(
                samples.orElseGet(this::defaultSamples), 27);
        try {
            return Optional.of(new VisibilityWatchDefinition(id.orElseThrow(), revision, target.orElseThrow(),
                    viewpoint, detection, occlusion, samplePolicy,
                    new VisibilityHysteresis(lostFrames, seenFrames), requireVisibleFirst,
                    edgeMarginPixels, concealment, evaluation, fingerprint));
        } catch (IllegalArgumentException exception) {
            invalid(diagnostics, exception.getMessage());
            return Optional.empty();
        }
    }

    private void readCall(
            InvocationExpressionSyntax call,
            VisibilityFieldCatalog fields,
            List<VisibilityDiagnostic> diagnostics) {
        String name = VisibilityCallSyntax.name(call);
        if (!readDefinitionCall(name, call, fields, diagnostics)
                && !readTimingCall(name, call, diagnostics)) {
            invalid(diagnostics, "Unsupported Visibility Init call: " + name);
        }
    }

    private boolean readDefinitionCall(
            String name,
            InvocationExpressionSyntax call,
            VisibilityFieldCatalog fields,
        List<VisibilityDiagnostic> diagnostics) {
        return switch (name) {
            case "Id" -> accepted(() -> readId(call, fields, diagnostics));
            case "Target" -> accepted(() -> readTarget(call, fields, diagnostics));
            case "Viewpoint" -> accepted(() -> policies.viewpoint(call, diagnostics)
                    .ifPresent(value -> viewpoint = value));
            case "HiddenWhen" -> accepted(() -> policies.detection(call, diagnostics)
                    .ifPresent(value -> detection = value));
            case "Occlusion" -> accepted(() -> policies.occlusion(call, diagnostics)
                    .ifPresent(value -> occlusion = value));
            case "Samples" -> accepted(() -> samples = Optional.of(policies.samples(call, diagnostics)));
            default -> {
                yield false;
            }
        };
    }

    private boolean readTimingCall(
            String name,
            InvocationExpressionSyntax call,
        List<VisibilityDiagnostic> diagnostics) {
        return switch (name) {
            case "LostAfter" -> accepted(() -> policies.wrappedInt(call, "RenderedFrames", diagnostics)
                    .ifPresent(value -> lostFrames = value));
            case "SeenAfter" -> accepted(() -> policies.wrappedInt(call, "RenderedFrames", diagnostics)
                    .ifPresent(value -> seenFrames = value));
            case "EdgeMargin" -> accepted(() -> policies.wrappedInt(call, "Pixels", diagnostics)
                    .ifPresent(value -> edgeMarginPixels = value));
            case "Concealment" -> accepted(() -> policies.concealment(call, diagnostics)
                    .ifPresent(value -> concealment = value));
            case "Evaluate" -> accepted(() -> policies.evaluation(call, diagnostics)
                    .ifPresent(value -> evaluation = value));
            case "RequireVisibleFirst" -> accepted(() -> requireVisibleFirst = true);
            case "AllowInitiallyHidden" -> accepted(() -> requireVisibleFirst = false);
            default -> {
                yield false;
            }
        };
    }

    private static boolean accepted(Runnable action) {
        action.run();
        return true;
    }

    private void readId(
            InvocationExpressionSyntax call,
            VisibilityFieldCatalog fields,
            List<VisibilityDiagnostic> diagnostics) {
        Optional<String> value = VisibilityCallSyntax.positional(call, 0).flatMap(fields::resolve);
        if (value.isEmpty()) {
            invalid(diagnostics, "Id(...) requires a VisibilityWatchId field or namespace:path string.");
            return;
        }
        try {
            id = Optional.of(new VisibilityWatchId(value.orElseThrow()));
        } catch (IllegalArgumentException exception) {
            invalid(diagnostics, exception.getMessage());
        }
    }

    private void readTarget(
            InvocationExpressionSyntax call,
            VisibilityFieldCatalog fields,
            List<VisibilityDiagnostic> diagnostics) {
        Optional<InvocationExpressionSyntax> nested = VisibilityCallSyntax.positionalCall(call, 0);
        if (nested.isEmpty()) {
            invalid(diagnostics, "Target(...) requires Npc(...) or Box(...).");
            return;
        }
        target = targetBinder.bind(nested.orElseThrow(), fields, diagnostics);
    }

    private List<VisibilitySampleKind> defaultSamples() {
        return target.filter(VisibilityBoxTarget.class::isInstance).isPresent()
                ? VisibilitySamplePolicy.boxDefaults().kinds()
                : VisibilitySamplePolicy.npcDefaults().kinds();
    }

    private static void invalid(List<VisibilityDiagnostic> diagnostics, String message) {
        diagnostics.add(VisibilityDiagnostic.of(VisibilityDiagnosticCode.VISIBILITY_WATCH_INVALID, message));
    }
}
