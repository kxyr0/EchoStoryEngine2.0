package io.github.kxyr0.echoitemattachments.core.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import org.junit.jupiter.api.Test;

class CommandReplayGuardTest {
    @Test
    void acceptsExactReplayAndRejectsChangedArguments() {
        CommandReplayGuard guard = new CommandReplayGuard();
        AttachmentCommandId id = new AttachmentCommandId("test:command");
        assertTrue(guard.accepts(id, "take|book|right"));
        assertTrue(guard.accepts(id, "take|book|right"));
        assertFalse(guard.accepts(id, "take|diamond|right"));
    }
}
