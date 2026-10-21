package io.github.kxyr0.echostoryengine.story.echoscript.environment.intrinsic;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.BlockPropertyDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.ClearBlockChangeDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentChangeDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentPosition;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentResourceDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentStateDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.SetBlockChangeDef;
import io.github.kxyr0.echostoryengine.story.echoscript.environment.EnvironmentSourceDiagnostics;
import io.github.kxyr0.echostoryengine.story.echoscript.environment.EnvironmentValue;
import java.util.List;
import java.util.Optional;

/**
 * Builds neutral environment values from EchoScript intrinsic calls.
 */
public final class EnvironmentIntrinsicFactory {
    private final EnvironmentIntrinsicReader reader;

    public EnvironmentIntrinsicFactory(
            EnvironmentSourceDiagnostics.SourceContext source,
            EnvironmentSourceDiagnostics diagnostics,
            List<StoryDiagnostic> values) {
        this.reader = new EnvironmentIntrinsicReader(source, diagnostics, values);
    }

    public EnvironmentValue create(String target, List<ArgumentValue> arguments, SourceSpan span) {
        return switch (target) {
            case "Environment" -> environment(arguments, span);
            case "State" -> state(arguments, span);
            case "Position" -> position(arguments, span);
            case "SetBlock" -> setBlock(arguments, span);
            case "ClearBlock" -> clearBlock(arguments, span);
            case "Properties" -> properties(arguments, span);
            case "Property" -> property(arguments, span);
            default -> reader.unknown(target, span);
        };
    }

    public boolean isIntrinsic(String target) {
        return switch (target) {
            case "Environment", "State", "Position", "SetBlock", "ClearBlock", "Properties", "Property" -> true;
            default -> false;
        };
    }

    private EnvironmentValue environment(List<ArgumentValue> arguments, SourceSpan span) {
        if (!reader.count("Environment", arguments, span, 3)) {
            return invalid();
        }
        Optional<NamespacedId> id = reader.id(reader.text(arguments.get(0), "environmentId"),
                arguments.get(0).span());
        Optional<NamespacedId> dimension = reader.id(reader.text(arguments.get(1), "dimension"),
                arguments.get(1).span());
        List<EnvironmentStateDef> states = reader.states(arguments.get(2).value(), arguments.get(2).span());
        if (id.isEmpty() || dimension.isEmpty() || states.isEmpty()) {
            return invalid();
        }
        return new EnvironmentValue.Resource(new EnvironmentResourceDef(
                id.orElseThrow(), dimension.orElseThrow(), states, reader.location(span)));
    }

    private EnvironmentValue state(List<ArgumentValue> arguments, SourceSpan span) {
        if (!reader.count("State", arguments, span, 2)) {
            return invalid();
        }
        String id = reader.text(arguments.get(0), "stateId");
        List<EnvironmentChangeDef> changes = reader.changes(arguments.get(1).value(), arguments.get(1).span());
        if (id.isBlank()) {
            return invalid();
        }
        return new EnvironmentValue.State(new EnvironmentStateDef(id, changes, reader.location(span)));
    }

    private EnvironmentValue position(List<ArgumentValue> arguments, SourceSpan span) {
        if (!reader.count("Position", arguments, span, 3)) {
            return invalid();
        }
        int x = reader.number(arguments.get(0), "x");
        int y = reader.number(arguments.get(1), "y");
        int z = reader.number(arguments.get(2), "z");
        return new EnvironmentValue.Position(new EnvironmentPosition(x, y, z));
    }

    private EnvironmentValue setBlock(List<ArgumentValue> arguments, SourceSpan span) {
        if (!reader.countRange("SetBlock", arguments, span, new EnvironmentIntrinsicReader.CountRange(2, 4))) {
            return invalid();
        }
        EnvironmentPosition position = reader.positionValue(arguments.get(0), "position");
        Optional<NamespacedId> blockId = reader.id(reader.text(arguments.get(1), "blockId"), arguments.get(1).span());
        List<BlockPropertyDef> properties = arguments.size() >= 3
                ? reader.properties(arguments.get(2).value(), arguments.get(2).span())
                : List.of();
        Optional<String> nbt = arguments.size() == 4
                ? Optional.of(reader.text(arguments.get(3), "nbt"))
                : Optional.empty();
        if (blockId.isEmpty()) {
            return invalid();
        }
        return new EnvironmentValue.Change(new SetBlockChangeDef(
                position, blockId.orElseThrow(), properties, nbt, reader.location(span)));
    }

    private EnvironmentValue clearBlock(List<ArgumentValue> arguments, SourceSpan span) {
        if (!reader.count("ClearBlock", arguments, span, 1)) {
            return invalid();
        }
        EnvironmentPosition position = reader.positionValue(arguments.get(0), "position");
        return new EnvironmentValue.Change(new ClearBlockChangeDef(position, reader.location(span)));
    }

    private EnvironmentValue properties(List<ArgumentValue> arguments, SourceSpan span) {
        List<BlockPropertyDef> properties = reader.propertyList(arguments, span);
        return new EnvironmentValue.Properties(properties);
    }

    private EnvironmentValue property(List<ArgumentValue> arguments, SourceSpan span) {
        if (!reader.count("Property", arguments, span, 2)) {
            return invalid();
        }
        String name = reader.text(arguments.get(0), "name");
        String value = reader.text(arguments.get(1), "value");
        return new EnvironmentValue.Property(new BlockPropertyDef(name, value, reader.location(span)));
    }

    private EnvironmentValue invalid() {
        return new EnvironmentValue.Invalid();
    }

    public record ArgumentValue(Optional<String> name, EnvironmentValue value, SourceSpan span) {
    }
}
