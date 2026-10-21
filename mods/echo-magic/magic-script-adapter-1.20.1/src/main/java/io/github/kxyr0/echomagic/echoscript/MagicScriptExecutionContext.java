package io.github.kxyr0.echomagic.echoscript;

import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import java.util.Optional;

public interface MagicScriptExecutionContext {
    Optional<MagicPlayerRef> currentPlayer();
    String deterministicCommandId();
}

