package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.audio;

import io.github.kxyr0.echoaudio.script.AudioCommandBinder;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.NodeSpec;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.linear.LinearNodeChain;
import java.util.Optional;

/** Frontend sugar only; playback belongs to the external echo_audio:cue adapter. */
public final class AudioActionIntrinsics {
    public Optional<LinearNodeChain.Entry> bind(InvocationExpressionSyntax call) {
        var cue = new AudioCommandBinder().bind(call);
        return Optional.of(new LinearNodeChain.Entry("sound", NodeSpec.action(call.span(),
                new NodeSpec.ActionFields(Optional.empty(), Optional.of("echo_audio:cue"),
                        Optional.empty(), cue.payload()))));
    }
}
