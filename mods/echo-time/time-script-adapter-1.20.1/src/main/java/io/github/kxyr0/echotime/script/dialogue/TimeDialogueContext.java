package io.github.kxyr0.echotime.script.dialogue;

import io.github.kxyr0.echodialogue.api.command.DialogueCommandPayload;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.identity.TimePauseOwner;
import io.github.kxyr0.echotime.api.identity.TimeWaitId;
import io.github.kxyr0.echotime.api.mutation.TimeRequestSource;
import io.github.kxyr0.echotime.script.action.TimeActionContext;
import java.util.Map;
import java.util.Optional;

record TimeDialogueContext(DialogueSessionView session, DialogueExternalWait externalWait) {
    TimeActionContext actionContext() {
        String identity = "dialogue:" + externalWait.waitKey();
        String sessionId = session.sessionId().value();
        TimeRequestSource source = new TimeRequestSource("echo_time", "dialogue", externalWait.waitKey(), sessionId,
                Optional.of(session.dialogueId().toString()), Optional.of(externalWait.commandPosition() + 1),
                "EchoScript Time command");
        return new TimeActionContext(new TimeMutationId(identity), new TimePauseOwner("dialogue:" + sessionId),
                new TimeWaitId(identity), source);
    }

    Map<String, String> payload() { return DialogueCommandPayload.decode(externalWait.payload()); }
}
