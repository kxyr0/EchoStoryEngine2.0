package io.github.kxyr0.echomagic.echoscript;

import io.github.kxyr0.echomagic.api.diagnostic.MagicDiagnostic;
import io.github.kxyr0.echomagic.api.diagnostic.MagicDiagnosticCode;
import io.github.kxyr0.echomagic.api.identity.SpellCastCommandId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import io.github.kxyr0.echomagic.forge.api.MagicService;
import java.util.Optional;

public final class MagicScriptExecutor {
    private final MagicService service;

    public MagicScriptExecutor(MagicService service) { this.service = service; }

    public ExecutionResult execute(MagicCommandBinding command, MagicScriptExecutionContext context) {
        Optional<MagicPlayerRef> current = context.currentPlayer();
        if (current.isEmpty()) return ExecutionResult.failure(MagicDiagnostic.error(
                MagicDiagnosticCode.MAGIC_PLAYER_CONTEXT_MISSING, "EchoScript player context is missing.", "Magic"));
        MagicPlayerRef player = current.orElseThrow();
        boolean value = switch (command.kind()) {
            case GRANT_TO_PLAYER -> service.grant(player, command.spellId());
            case REMOVE_FROM_PLAYER -> service.remove(player, command.spellId());
            case SELECT_FOR_PLAYER -> service.select(player, command.spellId());
            case CAST_AS_PLAYER -> service.cast(player, Optional.of(command.spellId()),
                    new SpellCastCommandId(context.deterministicCommandId())).accepted();
            case HAS_SPELL_ON_PLAYER -> service.knows(player, command.spellId());
            case SELECTED_SPELL_IS -> service.selectedSpell(player).selected().filter(command.spellId()::equals).isPresent();
        };
        return new ExecutionResult(true, value, Optional.empty());
    }

    public record ExecutionResult(boolean completed, boolean value, Optional<MagicDiagnostic> diagnostic) {
        public static ExecutionResult failure(MagicDiagnostic diagnostic) {
            return new ExecutionResult(true, false, Optional.of(diagnostic));
        }
    }
}
