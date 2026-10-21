package io.github.kxyr0.echobackgroundnpc.core.render;

import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundAppearanceId;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Revision-aware immutable skin/material resource cache shared by all agents. */
public final class SharedSkinResources<T> {
    private final Map<Key, T> resources = new HashMap<>();

    public T resolve(BackgroundAppearanceId appearance, long revision, Function<BackgroundAppearanceId, T> loader) {
        resources.keySet().removeIf(key -> key.appearance().equals(appearance) && key.revision() != revision);
        return resources.computeIfAbsent(new Key(appearance, revision), unused -> loader.apply(appearance));
    }

    public int size() { return resources.size(); }
    private record Key(BackgroundAppearanceId appearance, long revision) { }
}
