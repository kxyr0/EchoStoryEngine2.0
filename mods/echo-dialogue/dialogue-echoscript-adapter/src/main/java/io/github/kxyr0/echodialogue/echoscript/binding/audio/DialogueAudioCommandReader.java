package io.github.kxyr0.echodialogue.echoscript.binding.audio;

import io.github.kxyr0.echoaudio.api.AudioFailure;
import io.github.kxyr0.echoaudio.script.AudioCommandBinder;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Optional;

/** Преобразует общий звуковой DSL в команду на границе реплик. */
public final class DialogueAudioCommandReader {
    public Optional<CompiledDialogueCommand> read(InvocationExpressionSyntax call, int position,
            List<DialogueDiagnostic> diagnostics) {
        try {
            var cue = new AudioCommandBinder().bind(call);
            return Optional.of(new CompiledDialogueCommand("echo_audio:cue", position, cue.payload()));
        } catch (AudioFailure failure) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic(failure.code(), failure.getMessage(), call));
            return Optional.empty();
        }
    }
}
