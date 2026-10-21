package io.github.kxyr0.echoanimation.echoscript.binding;

import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import java.util.List;
import java.util.Optional;

/**
 * Finds methods by authored name.
 */
final class MethodLookup {
    private final List<MethodSyntax> methods;

    MethodLookup(List<MemberSyntax> members) {
        this.methods = members.stream().filter(MethodSyntax.class::isInstance).map(MethodSyntax.class::cast).toList();
    }

    Optional<MethodSyntax> unique(String name) {
        List<MethodSyntax> matches = methods.stream().filter(method -> method.name().equals(name)).toList();
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }
}

