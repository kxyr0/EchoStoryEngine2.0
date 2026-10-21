package io.github.kxyr0.echodialogue.echoscript.binding.command.external;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;
import io.github.kxyr0.echodialogue.echoscript.binding.command.core.CommandChecks;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PlayerCommandReader {
    private final ExternalArguments arguments = new ExternalArguments();

    public Optional<CompiledDialogueCommand> read(
            InvocationExpressionSyntax call,
            int position,
            String waitType,
            List<DialogueDiagnostic> diagnostics) {
        Map<String, String> values = arguments.player(call, waitType);
        Optional<String> error = validationError(waitType, values);
        if (error.isPresent()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(
                    "EDLG-SEM-049",
                    error.orElseThrow(),
                    call));
            return Optional.empty();
        }
        return Optional.of(new CompiledDialogueCommand(waitType, position, values));
    }

    private Optional<String> validationError(
            String waitType,
            Map<String, String> values) {
        return switch (waitType) {
            case "echo_player_actor:rotate" -> CommandChecks.errorUnless(
                    CommandChecks.required(values, "yaw", "pitch"),
                    "Player.Rotate requires yaw and pitch arguments.");
            case "echo_player_actor:spawn_at" -> CommandChecks.errorUnless(
                    CommandChecks.required(values, "x", "y", "z", "yaw", "pitch"),
                    "Player.SpawnAt requires x, y, z, yaw and pitch arguments.");
            case "echo_player_actor:set_hotbar_item" -> CommandChecks.errorUnless(
                    CommandChecks.required(values, "slot", "itemId"),
                    "Player.SetHotbarItem requires slot and itemId arguments.");
            case "echo_player_actor:select_hotbar_slot" -> CommandChecks.errorUnless(
                    CommandChecks.required(values, "slot"),
                    "Player.SelectHotbarSlot requires slot argument.");
            case "echo_player_vitals:set_fatigue" -> CommandChecks.errorUnless(
                    CommandChecks.nonNegativeValue(values),
                    "Player.SetFatigue requires non-negative integer value argument.");
            case "echo_player_vitals:add_fatigue" -> CommandChecks.errorUnless(
                    CommandChecks.nonNegativeValue(values),
                    "Player.AddFatigue requires non-negative integer value argument.");
            case "echo_player_vitals:reduce_fatigue" -> CommandChecks.errorUnless(
                    CommandChecks.nonNegativeValue(values),
                    "Player.ReduceFatigue requires non-negative integer value argument.");
            case "echo_player_actor:look_at", "echo_player_actor:sit_at",
                    "echo_player_actor:move", "echo_player_actor:interact" -> CommandChecks.errorUnless(
                            CommandChecks.required(values, "x", "y", "z"),
                            "Player command requires x, y and z arguments.");
            default -> Optional.empty();
        };
    }
}
