package io.github.kxyr0.echoobservation.echoscript.audio;

import io.github.kxyr0.echoaudio.api.AudioCue;
import io.github.kxyr0.echoaudio.script.AudioCommandBinder;
import io.github.kxyr0.echoobservation.api.action.ObservationScriptAction;
import io.github.kxyr0.echoobservation.api.action.ObservationScriptArgument;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.Optional;

/** OnComplete is fire-and-continue; a finite timer owns loops after the completion block ends. */
public final class ObservationAudioBinder {
    public ObservationScriptAction bind(InvocationExpressionSyntax call) {
        AudioCue cue = new AudioCommandBinder().bind(call);
        cue.requireCompletionSafe();
        return new ObservationScriptAction("echo_audio:cue", cue.payload().entrySet().stream()
                .map(entry -> new ObservationScriptArgument(Optional.of(entry.getKey()), entry.getValue())).toList(),
                call.span().startOffset());
    }
}
