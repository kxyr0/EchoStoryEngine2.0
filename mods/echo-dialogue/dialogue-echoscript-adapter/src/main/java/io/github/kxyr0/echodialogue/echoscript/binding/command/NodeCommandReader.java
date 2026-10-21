package io.github.kxyr0.echodialogue.echoscript.binding.command;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;
import io.github.kxyr0.echodialogue.echoscript.binding.command.core.CompiledCommandValidator;
import io.github.kxyr0.echodialogue.echoscript.binding.command.external.CutsceneCommandReader;
import io.github.kxyr0.echodialogue.echoscript.binding.command.external.DelayCommandReader;
import io.github.kxyr0.echodialogue.echoscript.binding.command.external.EffectCommandReader;
import io.github.kxyr0.echodialogue.echoscript.binding.command.external.PlayerCommandReader;
import io.github.kxyr0.echodialogue.echoscript.binding.command.external.ScreenCommandReader;
import io.github.kxyr0.echodialogue.echoscript.binding.command.external.TimeCommandReader;
import io.github.kxyr0.echodialogue.echoscript.binding.command.npc.NpcAppearanceCommandReader;
import io.github.kxyr0.echodialogue.echoscript.binding.command.npc.NpcInteractionCommandReader;
import io.github.kxyr0.echodialogue.echoscript.binding.command.npc.NpcMovementCommandReader;
import io.github.kxyr0.echodialogue.echoscript.binding.command.npc.NpcSleepCommandReader;
import io.github.kxyr0.echodialogue.echoscript.binding.command.visual.BlinkTimingCommandReader;
import io.github.kxyr0.echodialogue.echoscript.binding.command.visual.FaceAnimationCommandReader;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class NodeCommandReader {
    private final NpcMovementCommandReader movement = new NpcMovementCommandReader();
    private final NpcInteractionCommandReader interaction = new NpcInteractionCommandReader();
    private final NpcAppearanceCommandReader appearance = new NpcAppearanceCommandReader();
    private final NpcSleepCommandReader sleep = new NpcSleepCommandReader();
    private final DelayCommandReader delay = new DelayCommandReader();
    private final CutsceneCommandReader cutscene = new CutsceneCommandReader();
    private final PlayerCommandReader player = new PlayerCommandReader();
    private final ScreenCommandReader screen = new ScreenCommandReader();
    private final EffectCommandReader effect = new EffectCommandReader();
    private final TimeCommandReader time = new TimeCommandReader();
    private final FaceAnimationCommandReader face = new FaceAnimationCommandReader();
    private final BlinkTimingCommandReader blink = new BlinkTimingCommandReader();
    private final CompiledCommandValidator validator = new CompiledCommandValidator();

    public Optional<CompiledDialogueCommand> move(
            InvocationExpressionSyntax call, int position,
            Function<String, String> actorResolver,
            List<DialogueDiagnostic> diagnostics) {
        return movement.move(call, position, actorResolver, diagnostics, false);
    }

    public Optional<CompiledDialogueCommand> sprint(
            InvocationExpressionSyntax call, int position,
            Function<String, String> actorResolver,
            List<DialogueDiagnostic> diagnostics) {
        return movement.move(call, position, actorResolver, diagnostics, true);
    }

    public Optional<CompiledDialogueCommand> stop(
            InvocationExpressionSyntax call, int position,
            Function<String, String> actorResolver,
            List<DialogueDiagnostic> diagnostics) {
        return movement.stop(call, position, actorResolver, diagnostics);
    }

    public Optional<CompiledDialogueCommand> moveArrival(
            InvocationExpressionSyntax call, int position,
            Function<String, String> actorResolver,
            List<DialogueDiagnostic> diagnostics) {
        return movement.arrival(call, position, actorResolver, diagnostics);
    }

    public Optional<CompiledDialogueCommand> interact(
            InvocationExpressionSyntax call, int position,
            Function<String, String> actorResolver,
            Function<String, Optional<Map<String, String>>> blockPositionResolver,
            List<DialogueDiagnostic> diagnostics) {
        return interaction.interact(
                call, position, actorResolver, blockPositionResolver, diagnostics);
    }

    public Optional<CompiledDialogueCommand> stopInteraction(
            InvocationExpressionSyntax call, int position,
            Function<String, String> actorResolver,
            List<DialogueDiagnostic> diagnostics) {
        return interaction.stop(call, position, actorResolver, diagnostics);
    }

    public Optional<CompiledDialogueCommand> standUp(
            InvocationExpressionSyntax call, int position,
            Function<String, String> actorResolver,
            List<DialogueDiagnostic> diagnostics) {
        return interaction.standUp(call, position, actorResolver, diagnostics);
    }

    public Optional<CompiledDialogueCommand> setForm(
            InvocationExpressionSyntax call, int position,
            Function<String, String> actorResolver,
            Function<String, Optional<String>> fieldResolver,
            List<DialogueDiagnostic> diagnostics) {
        return appearance.setForm(
                call, position, actorResolver, fieldResolver, diagnostics);
    }

    public Optional<CompiledDialogueCommand> changeSkin(
            InvocationExpressionSyntax call, int position,
            Function<String, String> actorResolver,
            Function<String, Optional<String>> fieldResolver,
            List<DialogueDiagnostic> diagnostics) {
        return appearance.changeSkin(
                call, position, actorResolver, fieldResolver, diagnostics);
    }

    public Optional<CompiledDialogueCommand> sleepAt(
            InvocationExpressionSyntax call, int position,
            Function<String, String> actorResolver,
            Function<String, Optional<String>> fieldResolver,
            List<DialogueDiagnostic> diagnostics) {
        return sleep.sleepAt(call, position, actorResolver, fieldResolver, diagnostics);
    }

    public Optional<CompiledDialogueCommand> wakeUp(
            InvocationExpressionSyntax call, int position,
            Function<String, String> actorResolver,
            List<DialogueDiagnostic> diagnostics) {
        return sleep.wakeUp(call, position, actorResolver, diagnostics);
    }

    public Optional<CompiledDialogueCommand> delay(
            InvocationExpressionSyntax call, int position,
            List<DialogueDiagnostic> diagnostics) {
        return delay.read(call, position, diagnostics);
    }

    public Optional<CompiledDialogueCommand> cutscene(
            InvocationExpressionSyntax call, int position, String action,
            Function<String, Optional<String>> fieldResolver,
            List<DialogueDiagnostic> diagnostics) {
        return cutscene.read(call, position, action, fieldResolver, diagnostics);
    }

    public Optional<CompiledDialogueCommand> player(
            InvocationExpressionSyntax call, int position, String waitType,
            List<DialogueDiagnostic> diagnostics) {
        return player.read(call, position, waitType, diagnostics);
    }

    public Optional<CompiledDialogueCommand> screen(
            InvocationExpressionSyntax call, int position, String waitType,
            List<DialogueDiagnostic> diagnostics) {
        return screen.read(call, position, waitType, diagnostics);
    }

    public Optional<CompiledDialogueCommand> faceAnimation(
            InvocationExpressionSyntax call, int position,
            Function<String, String> subjectResolver,
            Function<String, Optional<String>> fieldResolver,
            List<DialogueDiagnostic> diagnostics) {
        return face.set(call, position, subjectResolver, fieldResolver, diagnostics);
    }

    public Optional<CompiledDialogueCommand> faceAnimationClear(
            InvocationExpressionSyntax call, int position,
            Function<String, String> subjectResolver, boolean all,
            List<DialogueDiagnostic> diagnostics) {
        return face.clear(call, position, subjectResolver, all, diagnostics);
    }

    public Optional<CompiledDialogueCommand> blinkTiming(
            InvocationExpressionSyntax call, int position,
            Function<String, String> subjectResolver, boolean clear,
            List<DialogueDiagnostic> diagnostics) {
        return blink.read(call, position, subjectResolver, clear, diagnostics);
    }

    public Optional<CompiledDialogueCommand> effectApplyToPlayer(
            InvocationExpressionSyntax call, int position,
            List<DialogueDiagnostic> diagnostics) {
        return effect.applyToPlayer(call, position, diagnostics);
    }

    public Optional<CompiledDialogueCommand> time(
            InvocationExpressionSyntax call, int position, String command,
            List<DialogueDiagnostic> diagnostics) {
        return time.read(call, position, command, diagnostics);
    }

    public List<CompiledDialogueCommand> valid(
            List<CompiledDialogueCommand> commands,
            int lineCount,
            List<DialogueDiagnostic> diagnostics) {
        return validator.valid(commands, lineCount, diagnostics);
    }
}
