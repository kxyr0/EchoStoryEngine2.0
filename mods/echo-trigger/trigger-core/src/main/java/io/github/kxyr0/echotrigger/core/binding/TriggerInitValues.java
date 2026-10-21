package io.github.kxyr0.echotrigger.core.binding;

import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackKind;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.model.TriggerDiagnostic;
import io.github.kxyr0.echotrigger.api.policy.TriggerFirePolicy;
import io.github.kxyr0.echotrigger.api.policy.TriggerManualFirePolicy;
import io.github.kxyr0.echotrigger.api.policy.TriggerMode;
import io.github.kxyr0.echotrigger.api.policy.TriggerScope;
import io.github.kxyr0.echotrigger.api.policy.TriggerStayPolicy;
import io.github.kxyr0.echotrigger.api.status.TriggerDiagnosticCode;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import io.github.kxyr0.echotrigger.core.binding.policy.TriggerInitPolicyReader;
import io.github.kxyr0.echotrigger.core.binding.subscription.TriggerStorySubscriptionState;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

final class TriggerInitValues {
    private Optional<TriggerId> id = Optional.empty();
    private TriggerScope scope = TriggerScope.PLAYER;
    private TriggerMode mode = TriggerMode.ONCE;
    private boolean explicitMode;
    private Optional<String> startMethod = Optional.empty();
    private long cooldownTicks;
    private TriggerManualFirePolicy manualFire = TriggerManualFirePolicy.DENY;
    private Optional<String> dimension = Optional.empty();
    private Optional<String> boundAreaName = Optional.empty();
    private int bindCount;
    private boolean fireOnlyOnce;
    private OptionalInt fireCount = OptionalInt.empty();
    private final EnumSet<TriggerCallbackKind> oneShotCallbacks = EnumSet.noneOf(TriggerCallbackKind.class);
    private long stayIntervalTicks = 20L;
    private TriggerStayPolicy stayPolicy = TriggerStayPolicy.SKIP_WHILE_RUNNING;
    private final TriggerInitPolicyReader policyReader = new TriggerInitPolicyReader();
    private final TriggerStorySubscriptionState storySubscriptionState = new TriggerStorySubscriptionState();

    static Optional<TriggerInitValues> read(
            MethodSyntax init, TriggerFieldCatalog fields,
            List<TriggerDiagnostic> diagnostics) {
        TriggerInitValues values = new TriggerInitValues();
        init.body().statements().stream()
                .flatMap(statement -> TriggerCallSyntax.statementCall(statement).stream())
                .forEach(call -> values.readCall(call, fields, diagnostics));
        if (values.id().isEmpty()) {
            diagnostics.add(diagnostic(TriggerDiagnosticCode.INVALID_ID,
                    "TriggerResource Init() must declare Id(\"namespace:path\")."));
            return Optional.empty();
        }
        values.storySubscriptionState.validate(diagnostics);
        return Optional.of(values);
    }

    void normalizeReactive(boolean reactive, List<TriggerDiagnostic> diagnostics) {
        if (reactive && !explicitMode) {
            mode = TriggerMode.REPEAT;
        }
        if (reactive && fireOnlyOnce && explicitMode && mode == TriggerMode.REPEAT) {
            diagnostics.add(diagnostic(TriggerDiagnosticCode.TRIGGER_FIRE_POLICY_CONFLICT,
                    "FireOnlyOnce conflicts with explicit Mode(REPEAT)."));
        }
        if (reactive && fireOnlyOnce && fireCount.isPresent()) {
            diagnostics.add(diagnostic(TriggerDiagnosticCode.TRIGGER_FIRE_POLICY_CONFLICT,
                    "FireOnlyOnce conflicts with FireCount."));
        }
    }

    TriggerFirePolicy firePolicy() {
        return new TriggerFirePolicy(fireOnlyOnce, oneShotCallbacks, fireCount, cooldownTicks);
    }

    Optional<TriggerId> id() { return id; }
    TriggerScope scope() { return scope; }
    TriggerMode mode() { return mode; }
    Optional<String> startMethod() { return startMethod; }
    long cooldownTicks() { return cooldownTicks; }
    TriggerManualFirePolicy manualFire() { return manualFire; }
    Optional<String> dimension() { return dimension; }
    Optional<String> boundAreaName() { return boundAreaName; }
    int bindCount() { return bindCount; }
    long stayIntervalTicks() { return stayIntervalTicks; }
    TriggerStayPolicy stayPolicy() { return stayPolicy; }
    boolean hasStorySubscription() { return storySubscriptionState.has(); }

    Optional<TriggerStorySubscription> storySubscription() {
        return id.flatMap(triggerId -> storySubscriptionState.toSubscription(triggerId, fireOnlyOnce));
    }

    private void readCall(
            InvocationExpressionSyntax call, TriggerFieldCatalog fields,
            List<TriggerDiagnostic> diagnostics) {
        String name = TriggerCallSyntax.name(call);
        if (readCoreCall(name, call, fields, diagnostics) || readBindingCall(name, call)) {
            return;
        }
        readPolicyCall(name, call, diagnostics);
    }

    private boolean readCoreCall(
            String name,
            InvocationExpressionSyntax call, TriggerFieldCatalog fields,
            List<TriggerDiagnostic> diagnostics) {
        switch (name) {
            case "Id" -> readId(call, diagnostics);
            case "Scope" -> policyReader.readScope(call, diagnostics).ifPresent(value -> scope = value);
            case "Mode" -> policyReader.readMode(call, diagnostics).ifPresent(value -> {
                mode = value;
                explicitMode = true;
            });
            case "Start" -> TriggerCallSyntax.positionalName(call, 0)
                    .ifPresent(value -> startMethod = Optional.of(value));
            case "Subscribe" -> readSubscribe(call, fields, diagnostics);
            case "StartStory" -> readStartStory(call, fields, diagnostics);
            case "StartDialogue" -> readStartDialogue(call, fields, diagnostics);
            case "AllowManualFire" -> manualFire = TriggerManualFirePolicy.ALLOW;
            case "Cooldown" -> policyReader.readTicks(call).ifPresent(value -> cooldownTicks = value);
            default -> {
                return false;
            }
        }
        return true;
    }

    private boolean readBindingCall(String name, InvocationExpressionSyntax call) {
        switch (name) {
            case "Dimension" -> TriggerCallSyntax.positionalString(call, 0)
                    .ifPresent(value -> dimension = Optional.of(value));
            case "Bind" -> readBind(call);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void readPolicyCall(
            String name, InvocationExpressionSyntax call,
            List<TriggerDiagnostic> diagnostics) {
        switch (name) {
            case "FireOnlyOnce", "FireOnlyOncePerPlayer", "FireOnlyOncePerArm" -> fireOnlyOnce = true;
            case "EnterOnlyOnce" -> oneShotCallbacks.add(TriggerCallbackKind.ENTER);
            case "ExitOnlyOnce" -> oneShotCallbacks.add(TriggerCallbackKind.EXIT);
            case "StayOnlyOnce" -> oneShotCallbacks.add(TriggerCallbackKind.STAY);
            case "FireCount" -> policyReader.readFireCount(call, diagnostics)
                    .ifPresent(value -> fireCount = OptionalInt.of(value));
            case "StayInterval" -> policyReader.readStayInterval(call, diagnostics)
                    .ifPresent(value -> stayIntervalTicks = value);
            case "StayPolicy" -> policyReader.readStayPolicy(call, diagnostics)
                    .ifPresent(value -> stayPolicy = value);
            default -> {
            }
        }
    }

    private void readId(InvocationExpressionSyntax call, List<TriggerDiagnostic> diagnostics) {
        TriggerCallSyntax.positionalString(call, 0).ifPresent(value -> {
            try {
                id = Optional.of(TriggerId.parse(value));
            } catch (IllegalArgumentException exception) {
                diagnostics.add(diagnostic(TriggerDiagnosticCode.INVALID_ID, exception.getMessage()));
            }
        });
    }

    private void readBind(InvocationExpressionSyntax call) {
        bindCount++;
        TriggerCallSyntax.positionalName(call, 0).ifPresent(value -> boundAreaName = Optional.of(value));
    }

    private void readSubscribe(
            InvocationExpressionSyntax call,
            TriggerFieldCatalog fields,
            List<TriggerDiagnostic> diagnostics) {
        storySubscriptionState.readSubscribe(call, fields::string, diagnostics);
    }

    private void readStartStory(
            InvocationExpressionSyntax call,
            TriggerFieldCatalog fields,
            List<TriggerDiagnostic> diagnostics) {
        storySubscriptionState.readStartStory(call, fields::string, diagnostics);
    }

    private void readStartDialogue(
            InvocationExpressionSyntax call,
            TriggerFieldCatalog fields,
            List<TriggerDiagnostic> diagnostics) {
        storySubscriptionState.readStartDialogue(call, fields::string, diagnostics);
    }

    private static TriggerDiagnostic diagnostic(TriggerDiagnosticCode code, String message) {
        return new TriggerDiagnostic(code, message);
    }
}
