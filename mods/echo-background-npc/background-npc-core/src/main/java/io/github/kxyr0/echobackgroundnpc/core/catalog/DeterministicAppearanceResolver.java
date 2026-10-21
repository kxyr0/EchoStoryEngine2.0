package io.github.kxyr0.echobackgroundnpc.core.catalog;

import io.github.kxyr0.echobackgroundnpc.api.definition.BackgroundAppearancePool;
import io.github.kxyr0.echobackgroundnpc.api.definition.BackgroundNpcDefinition;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundAppearanceId;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.CRC32;

/** Resolves one stable appearance without keeping duplicate per-agent asset data. */
public final class DeterministicAppearanceResolver {
    public static final BackgroundAppearanceId DEFAULT =
            new BackgroundAppearanceId("echo_background_npc:default");

    public BackgroundAppearanceId resolve(BackgroundNpcDefinition definition, BackgroundCatalog catalog) {
        if (definition.appearance().isPresent()) return definition.appearance().orElseThrow();
        Optional<BackgroundAppearancePool> direct = definition.appearancePool().map(catalog.appearancePools()::get);
        Optional<BackgroundAppearancePool> profile = definition.profile().map(catalog.profiles()::get)
                .flatMap(value -> value.appearancePool()).map(catalog.appearancePools()::get);
        return direct.or(() -> profile).map(pool -> select(definition, pool)).orElse(DEFAULT);
    }

    private BackgroundAppearanceId select(BackgroundNpcDefinition definition, BackgroundAppearancePool pool) {
        CRC32 crc = new CRC32();
        String seed = definition.id().value() + "#" + pool.id().value();
        crc.update(seed.getBytes(StandardCharsets.UTF_8));
        return pool.appearances().get((int) (crc.getValue() % pool.appearances().size()));
    }
}
