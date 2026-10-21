package io.github.kxyr0.echobackgroundnpc.echoscript.binding;

import io.github.kxyr0.echobackgroundnpc.api.definition.BackgroundAppearancePool;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundAppearanceId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundAppearancePoolId;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;

public final class AppearancePoolResourceBinder {
    private final BackgroundCallReader calls = new BackgroundCallReader();

    public BackgroundAppearancePool bind(ClassSyntax resource, BackgroundResourceContext context, long revision) {
        if (!resource.baseType().name().text().equals("BackgroundAppearancePoolResource")) {
            throw new IllegalArgumentException("Expected BackgroundAppearancePoolResource.");
        }
        var appearances = calls.calls(resource, "Skin").stream()
                .map(call -> calls.string(call, 0).orElseThrow(() -> new IllegalArgumentException(
                        "Skin requires one string."))).map(BackgroundAppearanceId::new).toList();
        return new BackgroundAppearancePool(new BackgroundAppearancePoolId(context.resourceId()), appearances,
                revision);
    }
}
