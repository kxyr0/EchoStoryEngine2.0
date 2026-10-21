package io.github.kxyr0.echoplayer.core.dialogue;

import io.github.kxyr0.echoplayer.api.identity.PlayerDisplayName;
import io.github.kxyr0.echoplayer.api.result.PlayerProfileDiagnostic;
import io.github.kxyr0.echoplayer.api.result.PlayerProfileResult;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.List;

/**
 * EchoScript AST binding for player dialogue-side calls.
 */
public final class PlayerDialogueCallBinder {
    public PlayerProfileResult<PlayerDisplayName> bindSetPlayerName(InvocationExpressionSyntax call, String source) {
        if (!"SetPlayerName".equals(callName(call))) {
            return invalid("PLAYER_DIALOGUE_CALL_UNSUPPORTED", "Only SetPlayerName(string) is supported.", source);
        }
        if (call.arguments().size() != 1 || call.arguments().get(0).name().isPresent()
                || !(call.arguments().get(0).value() instanceof StringLiteralExpressionSyntax literal)) {
            return invalid("PLAYER_DIALOGUE_CALL_ARGUMENTS",
                    "SetPlayerName requires exactly one positional string argument.", source);
        }
        try {
            return PlayerProfileResult.valid(PlayerDisplayName.of(literal.value()), List.of());
        } catch (IllegalArgumentException ex) {
            return invalid("PLAYER_DIALOGUE_NAME_INVALID", ex.getMessage(), source);
        }
    }

    private String callName(InvocationExpressionSyntax call) {
        if (call.target() instanceof NameExpressionSyntax name) {
            return name.name();
        }
        if (call.target() instanceof MemberAccessExpressionSyntax access) {
            return access.memberName();
        }
        return "";
    }

    private PlayerProfileResult<PlayerDisplayName> invalid(String code, String message, String source) {
        return PlayerProfileResult.invalid(List.of(PlayerProfileDiagnostic.error(code, message, source)));
    }
}
