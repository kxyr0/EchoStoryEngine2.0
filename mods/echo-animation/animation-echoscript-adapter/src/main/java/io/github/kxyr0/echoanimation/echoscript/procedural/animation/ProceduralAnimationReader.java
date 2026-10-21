package io.github.kxyr0.echoanimation.echoscript.procedural.animation;

import io.github.kxyr0.echoanimation.api.animation.definition.RecoveryPolicy;
import io.github.kxyr0.echoanimation.api.animation.definition.ReplayPolicy;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.echoscript.AnimationCompileResult;
import io.github.kxyr0.echoanimation.echoscript.diagnostic.AnimationSemanticDiagnostics;
import io.github.kxyr0.echoanimation.echoscript.procedural.ProceduralSyntax;
import io.github.kxyr0.echoanimation.echoscript.syntax.CallSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.LocalDeclarationStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.StatementSyntax;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Binds procedural AnimationResource Build statements into the existing lifecycle model. */
public final class ProceduralAnimationReader {
    public AnimationCompileResult read(MethodSyntax build, List<AnimationDiagnostic> diagnostics) {
        AnimationDslDraft draft = new AnimationDslDraft();
        try {
            for (StatementSyntax statement : build.body().statements()) {
                readStatement(draft, statement);
            }
            return new AnimationCompileResult( Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.of(draft.build()), Optional.empty(), diagnostics);
        } catch (RuntimeException exception) {
            diagnostics.add( AnimationSemanticDiagnostics.error(
                            "EANIM-ES-ANIMATION-BUILDER", exception.getMessage(), build.span()));
            return new AnimationCompileResult( Optional.empty(), Optional.empty(), Optional.empty(), diagnostics);
        }
    }

    private void readStatement(AnimationDslDraft draft, StatementSyntax statement) {
        if (statement instanceof LocalDeclarationStatementSyntax local) {
            ExpressionSyntax initializer = local.initializer() .orElseThrow( () ->
                                            new IllegalArgumentException( "Local '" + local.name()
                                                            + "' requires an initializer."));
            draft.local(local.name(), value(draft, initializer));
            return;
        }
        if (statement instanceof ExpressionStatementSyntax expression) {
            readCall(draft, ProceduralSyntax.invocation(expression.expression()));
            return;
        }
        throw new IllegalArgumentException( "Procedural animation Build supports only local declarations and calls.");
    }

    private AnimationDslValue value(AnimationDslDraft draft, ExpressionSyntax expression) {
        if (expression
                instanceof
                io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax name) {
            AnimationDslValue value = draft.local(name.name());
            return value instanceof AnimationDslValue.Clip clip ? new AnimationDslValue.Play(clip.id()) : value;
        }
        InvocationExpressionSyntax call = ProceduralSyntax.invocation(expression);
        return switch (CallSyntax.name(call)) {
            case "Channel" ->
                    new AnimationDslValue.Channel( new AnimationChannelId(ProceduralSyntax.string(call, 0, null)));
            case "State" -> state(draft, call);
            case "Clip" ->
                    new AnimationDslValue.Clip( AnimationClipId.parse(ProceduralSyntax.string(call, 0, null)));
            case "Segment" -> play(draft, call, false);
            case "Point" -> play(draft, call, true);
            default ->
                    throw new IllegalArgumentException(
                            "Unsupported animation value intrinsic: " + CallSyntax.name(call));
        };
    }

    private AnimationDslValue.State state( AnimationDslDraft draft, InvocationExpressionSyntax call) {
        AnimationDslValue channel = value(draft, ProceduralSyntax.argument(call, 0, null));
        if (!(channel instanceof AnimationDslValue.Channel typed)) {
            throw new IllegalArgumentException("State requires a Channel local.");
        }
        String name = ProceduralSyntax.string(call, 1, null);
        return new AnimationDslValue.State( typed.id(), new AnimationStateId(typed.id().value() + "/" + name));
    }

    private AnimationDslValue.Play play( AnimationDslDraft draft, InvocationExpressionSyntax call, boolean point) {
        String owner = ProceduralSyntax.owner(call) .orElseThrow( () ->
                                        new IllegalArgumentException( "Segment/Point must be called on a Clip local."));
        AnimationDslValue value = draft.local(owner);
        if (!(value instanceof AnimationDslValue.Clip clip)) {
            throw new IllegalArgumentException("'" + owner + "' is not a Clip local.");
        }
        String id = ProceduralSyntax.string(call, 0, null);
        return point ? new AnimationDslValue.Play(clip.id(), Optional.empty(), Optional.of(id))
                : new AnimationDslValue.Play(clip.id(), Optional.of(id), Optional.empty());
    }

    private void readCall(AnimationDslDraft draft, InvocationExpressionSyntax call) {
        switch (CallSyntax.name(call)) {
            case "Id" -> draft.id(ProceduralSyntax.string(call, 0, null));
            case "Enter" ->
                    draft.enter( stateValue(draft, call, 0, null), stateValue(draft, call, 1, "from"),
                            playValue(draft, call, 2, "play"));
            case "Loop" ->
                    draft.loop(stateValue(draft, call, 0, null), playValue(draft, call, 1, "play"));
            case "Hold" ->
                    draft.hold(stateValue(draft, call, 0, null), playValue(draft, call, 1, "pose"));
            case "OneShot" -> oneShot(draft, call);
            case "Exit" ->
                    draft.exit( stateValue(draft, call, 0, null), stateValue(draft, call, 1, "to"),
                            playValue(draft, call, 2, "play"), optionalPlay(draft, call, 3, "at"));
            case "ReturnTo" -> draft.returnTo(stateValue(draft, call, 0, null).id());
            default -> readPolicyCall(draft, call);
        }
    }

    private void readPolicyCall(AnimationDslDraft draft, InvocationExpressionSyntax call) {
        switch (CallSyntax.name(call)) {
            case "HoldLastFrame" -> draft.holdLastFrame();
            case "Marker" ->
                    draft.marker( ProceduralSyntax.string(call, 0, null),
                            io.github.kxyr0.echoanimation.api.animation.AnimationPhase.valueOf(
                                    ProceduralSyntax.enumSymbol(call, 1, "phase")), ProceduralSyntax.integer(call,
                2, "tick"));
            case "Recovery" ->
                    draft.recovery( RecoveryPolicy.valueOf(ProceduralSyntax.enumSymbol(call, 0, null)),
                            ProceduralSyntax.optionalInteger(call, 1, "checkpointEvery", 0));
            case "Replay" -> draft.replay(ReplayPolicy.valueOf(ProceduralSyntax.enumSymbol(call, 0, null)));
            case "Fallback" -> fallback(draft, call);
            case "Blend" -> blend(draft, call);
            case "BlendIn" -> draft.blendIn(ProceduralSyntax.integer(call, 0, null));
            case "BlendOut" -> draft.blendOut(ProceduralSyntax.integer(call, 0, null));
            case "Mask" -> mask(draft, call);
            default ->
                    throw new IllegalArgumentException(
                            "Unsupported animation builder intrinsic: " + CallSyntax.name(call));
        }
    }

    private void oneShot(AnimationDslDraft draft, InvocationExpressionSyntax call) {
        AnimationDslValue target = value(draft, ProceduralSyntax.argument(call, 0, null));
        AnimationChannelId channel = target instanceof AnimationDslValue.Channel typed ? typed.id()
                        : ((AnimationDslValue.State) target).channel();
        draft.oneShot(channel, playValue(draft, call, 1, "play"));
    }

    private void fallback(AnimationDslDraft draft, InvocationExpressionSyntax call) {
        value(draft, ProceduralSyntax.argument(call, 0, null));
        InvocationExpressionSyntax crossfade = ProceduralSyntax.invocation(ProceduralSyntax.argument(call, 1, null));
        if (!CallSyntax.named(crossfade, "Crossfade")) {
            throw new IllegalArgumentException("Fallback requires Crossfade(ticks).");
        }
        draft.fallback(ProceduralSyntax.integer(crossfade, 0, null));
    }

    private void blend(AnimationDslDraft draft, InvocationExpressionSyntax call) {
        value(draft, ProceduralSyntax.argument(call, 0, null));
        draft.blend(ProceduralSyntax.integer(call, 1, "ticks"));
    }

    private void mask(AnimationDslDraft draft, InvocationExpressionSyntax call) {
        value(draft, ProceduralSyntax.argument(call, 0, null));
        draft.mask(ProceduralSyntax.string(call, 1, null));
    }

    private AnimationDslValue.State stateValue(
            AnimationDslDraft draft, InvocationExpressionSyntax call, int index, String name) {
        AnimationDslValue value = value(draft, ProceduralSyntax.argument(call, index, name));
        if (value instanceof AnimationDslValue.State state) {
            return state;
        }
        throw new IllegalArgumentException( "Expected a State local for " + (name == null ? index : name) + ".");
    }

    private AnimationDslValue.Play playValue(
            AnimationDslDraft draft, InvocationExpressionSyntax call, int index, String name) {
        AnimationDslValue value = value(draft, ProceduralSyntax.argument(call, index, name));
        if (value instanceof AnimationDslValue.Play play) {
            return play;
        }
        throw new IllegalArgumentException("Expected a Clip, Segment or Point for '" + name + "'.");
    }

    private Optional<AnimationDslValue.Play> optionalPlay(
            AnimationDslDraft draft, InvocationExpressionSyntax call, int index, String name) {
        return ProceduralSyntax.optional(call, index, name) .map( expression -> {
                            AnimationDslValue value = value(draft, expression);
                            if (value instanceof AnimationDslValue.Play play) {
                                return play;
                            }
                            throw new IllegalArgumentException( "Expected a clip Point for '" + name + "'.");
                        });
    }

}
