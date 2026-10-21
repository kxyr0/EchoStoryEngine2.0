package io.github.kxyr0.echomagic.echoscript;

import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.Optional;

public final class MagicCommandBinder {
    public Optional<MagicCommandBinding> bind(InvocationExpressionSyntax call) {
        MagicCommandKind kind = switch (name(call)) {
            case "Magic.GrantToPlayer" -> MagicCommandKind.GRANT_TO_PLAYER;
            case "Magic.RemoveFromPlayer" -> MagicCommandKind.REMOVE_FROM_PLAYER;
            case "Magic.SelectForPlayer" -> MagicCommandKind.SELECT_FOR_PLAYER;
            case "Magic.CastAsPlayer" -> MagicCommandKind.CAST_AS_PLAYER;
            case "Magic.HasSpellOnPlayer" -> MagicCommandKind.HAS_SPELL_ON_PLAYER;
            case "Magic.SelectedSpellIs" -> MagicCommandKind.SELECTED_SPELL_IS;
            default -> null;
        };
        if (kind == null) return Optional.empty();
        if (call.arguments().isEmpty() || !(call.arguments().get(0).value() instanceof StringLiteralExpressionSyntax id)) {
            throw new IllegalArgumentException("Magic call requires a literal SpellId.");
        }
        return Optional.of(new MagicCommandBinding(kind, new SpellId(id.value())));
    }

    private String name(InvocationExpressionSyntax call) {
        if (call.target() instanceof MemberAccessExpressionSyntax access
                && access.target() instanceof NameExpressionSyntax qualifier) {
            return qualifier.name() + "." + access.memberName();
        }
        return "";
    }
}

