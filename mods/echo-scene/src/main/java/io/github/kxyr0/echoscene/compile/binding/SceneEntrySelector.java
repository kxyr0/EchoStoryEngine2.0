package io.github.kxyr0.echoscene.compile.binding;

import io.github.kxyr0.echoscript.api.syntax.SyntaxModifier;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import java.util.List;
import java.util.Optional;

final class SceneEntrySelector {
    Optional<MethodSyntax> select(List<MethodSyntax> scenes) {
        if (scenes.size() == 1) {
            return Optional.of(scenes.get(0));
        }
        List<MethodSyntax> starts = scenes.stream()
                .filter(method -> method.modifiers().contains(SyntaxModifier.START)).toList();
        return starts.size() == 1 ? Optional.of(starts.get(0)) : Optional.empty();
    }

    boolean ambiguous(List<MethodSyntax> scenes) {
        return scenes.size() > 1 && scenes.stream()
                .filter(method -> method.modifiers().contains(SyntaxModifier.START)).count() > 1;
    }
}
