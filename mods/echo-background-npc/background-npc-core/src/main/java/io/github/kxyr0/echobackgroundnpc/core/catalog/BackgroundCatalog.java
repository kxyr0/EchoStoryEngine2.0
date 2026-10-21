package io.github.kxyr0.echobackgroundnpc.core.catalog;

import io.github.kxyr0.echobackgroundnpc.api.conversation.AmbientDialogue;
import io.github.kxyr0.echobackgroundnpc.api.definition.BackgroundAppearancePool;
import io.github.kxyr0.echobackgroundnpc.api.definition.BackgroundNpcDefinition;
import io.github.kxyr0.echobackgroundnpc.api.definition.BackgroundNpcProfile;
import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.AmbientDialogueId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundAppearancePoolId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundNpcProfileId;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** One immutable, atomically replaceable resource revision. */
public record BackgroundCatalog(
        long revision,
        Map<BackgroundNpcId, BackgroundNpcDefinition> definitions,
        Map<BackgroundNpcProfileId, BackgroundNpcProfile> profiles,
        Map<BackgroundAppearancePoolId, BackgroundAppearancePool> appearancePools,
        Map<AmbientDialogueId, AmbientDialogue> dialogues
) {
    public BackgroundCatalog {
        if (revision < 0L) throw new IllegalArgumentException("revision cannot be negative.");
        definitions = Map.copyOf(Objects.requireNonNull(definitions, "definitions"));
        profiles = Map.copyOf(Objects.requireNonNull(profiles, "profiles"));
        appearancePools = Map.copyOf(Objects.requireNonNull(appearancePools, "appearancePools"));
        dialogues = Map.copyOf(Objects.requireNonNull(dialogues, "dialogues"));
    }

    public static BackgroundCatalog of(long revision, Collection<BackgroundNpcDefinition> definitions,
            Collection<BackgroundNpcProfile> profiles, Collection<BackgroundAppearancePool> pools,
            Collection<AmbientDialogue> dialogues) {
        return new BackgroundCatalog(revision, unique(definitions, BackgroundNpcDefinition::id),
                unique(profiles, BackgroundNpcProfile::id), unique(pools, BackgroundAppearancePool::id),
                unique(dialogues, AmbientDialogue::id));
    }

    private static <K, V> Map<K, V> unique(Collection<V> values, Function<V, K> key) {
        return values.stream().collect(Collectors.toUnmodifiableMap(key, Function.identity()));
    }
}
