package io.github.kxyr0.echobackgroundnpc.echoscript.binding;

import io.github.kxyr0.echobackgroundnpc.api.definition.BackgroundNpcProfile;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundAppearancePoolId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundNpcProfileId;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import java.util.Optional;

public final class ProfileResourceBinder {
    private final BackgroundCallReader calls = new BackgroundCallReader();

    public BackgroundNpcProfile bind(ClassSyntax resource, BackgroundResourceContext context, long revision) {
        requireBase(resource);
        Optional<BackgroundAppearancePoolId> pool = singleString(resource, "AppearancePool")
                .map(BackgroundAppearancePoolId::new);
        double speed = calls.calls(resource, "WalkSpeed").stream().findFirst()
                .flatMap(value -> calls.number(value, 0)).orElse(1.0);
        String locomotion = singleString(resource, "LocomotionProfile").orElse("jdh:standard_humanoid");
        return new BackgroundNpcProfile(new BackgroundNpcProfileId(context.resourceId()), pool, speed,
                locomotion, revision);
    }

    private Optional<String> singleString(ClassSyntax resource, String name) {
        var found = calls.calls(resource, name);
        if (found.size() > 1) throw new IllegalArgumentException(name + " declared more than once.");
        return found.stream().findFirst().flatMap(value -> calls.string(value, 0));
    }
    private void requireBase(ClassSyntax resource) {
        if (!resource.baseType().name().text().equals("BackgroundNpcProfileResource")) {
            throw new IllegalArgumentException("Expected BackgroundNpcProfileResource.");
        }
    }
}
