package io.github.kxyr0.echostoryengine.story.echoscript.environment;

import io.github.kxyr0.echostoryengine.story.definition.environment.model.BlockPropertyDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentChangeDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentPosition;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentResourceDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentStateDef;
import java.util.List;

public sealed interface EnvironmentValue permits
        EnvironmentValue.Invalid,
        EnvironmentValue.Text,
        EnvironmentValue.Number,
        EnvironmentValue.Array,
        EnvironmentValue.Position,
        EnvironmentValue.Property,
        EnvironmentValue.Properties,
        EnvironmentValue.Change,
        EnvironmentValue.State,
        EnvironmentValue.Resource {
    record Invalid() implements EnvironmentValue {
    }

    record Text(String value) implements EnvironmentValue {
    }

    record Number(int value) implements EnvironmentValue {
    }

    record Array(List<EnvironmentValue> values) implements EnvironmentValue {
        public Array {
            values = List.copyOf(values);
        }
    }

    record Position(EnvironmentPosition value) implements EnvironmentValue {
    }

    record Property(BlockPropertyDef value) implements EnvironmentValue {
    }

    record Properties(List<BlockPropertyDef> values) implements EnvironmentValue {
        public Properties {
            values = List.copyOf(values);
        }
    }

    record Change(EnvironmentChangeDef value) implements EnvironmentValue {
    }

    record State(EnvironmentStateDef value) implements EnvironmentValue {
    }

    record Resource(EnvironmentResourceDef value) implements EnvironmentValue {
    }
}
