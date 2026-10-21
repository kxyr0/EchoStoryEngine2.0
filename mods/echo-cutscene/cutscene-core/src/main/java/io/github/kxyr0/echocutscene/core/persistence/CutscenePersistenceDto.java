package io.github.kxyr0.echocutscene.core.persistence;

import java.util.Map;
import java.util.Objects;

public record CutscenePersistenceDto(String schema, Map<String, String> values) {
    public CutscenePersistenceDto {
        schema = Objects.requireNonNull(schema, "schema").trim();
        values = Map.copyOf(Objects.requireNonNull(values, "values"));
        if (schema.isEmpty()) {
            throw new IllegalArgumentException("schema must not be blank.");
        }
    }
}
