package io.github.kxyr0.echostoryengine.story.echoscript.environment.intrinsic;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.BlockPropertyDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentChangeDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentPosition;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentStateDef;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation;
import io.github.kxyr0.echostoryengine.story.echoscript.environment.EnvironmentSourceDiagnostics;
import io.github.kxyr0.echostoryengine.story.echoscript.environment.EnvironmentValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class EnvironmentIntrinsicReader {
    private final EnvironmentSourceDiagnostics.SourceContext source;
    private final EnvironmentSourceDiagnostics diagnostics;
    private final List<StoryDiagnostic> values;

    EnvironmentIntrinsicReader(
            EnvironmentSourceDiagnostics.SourceContext source,
            EnvironmentSourceDiagnostics diagnostics,
            List<StoryDiagnostic> values) {
        this.source = source;
        this.diagnostics = diagnostics;
        this.values = values;
    }

    List<EnvironmentStateDef> states(EnvironmentValue value, SourceSpan span) {
        List<EnvironmentStateDef> states = new ArrayList<>();
        for (EnvironmentValue item : array(value, "states", span)) {
            if (item instanceof EnvironmentValue.State state) {
                states.add(state.value());
            } else {
                typeError("states", "EnvironmentStateDef", span);
            }
        }
        return List.copyOf(states);
    }

    List<EnvironmentChangeDef> changes(EnvironmentValue value, SourceSpan span) {
        List<EnvironmentChangeDef> changes = new ArrayList<>();
        for (EnvironmentValue item : array(value, "changes", span)) {
            if (item instanceof EnvironmentValue.Change change) {
                changes.add(change.value());
            } else {
                typeError("changes", "EnvironmentChangeDef", span);
            }
        }
        return List.copyOf(changes);
    }

    List<BlockPropertyDef> properties(EnvironmentValue value, SourceSpan span) {
        if (value instanceof EnvironmentValue.Properties properties) {
            return properties.values();
        }
        List<BlockPropertyDef> properties = new ArrayList<>();
        for (EnvironmentValue item : array(value, "properties", span)) {
            properties.add(propertyValue(item, span));
        }
        return List.copyOf(properties);
    }

    List<BlockPropertyDef> propertyList(List<EnvironmentIntrinsicFactory.ArgumentValue> arguments, SourceSpan span) {
        List<BlockPropertyDef> properties = new ArrayList<>();
        for (EnvironmentIntrinsicFactory.ArgumentValue argument : arguments) {
            if (argument.value() instanceof EnvironmentValue.Array array) {
                properties.addAll(properties(array, argument.span()));
            } else {
                properties.add(propertyValue(argument.value(), argument.span()));
            }
        }
        return List.copyOf(properties);
    }

    EnvironmentPosition positionValue(EnvironmentIntrinsicFactory.ArgumentValue argument, String name) {
        if (argument.value() instanceof EnvironmentValue.Position position) {
            return position.value();
        }
        typeError(name, "Position", argument.span());
        return new EnvironmentPosition(0, 0, 0);
    }

    String text(EnvironmentIntrinsicFactory.ArgumentValue argument, String name) {
        if (argument.value() instanceof EnvironmentValue.Text text) {
            return text.value();
        }
        typeError(name, "string", argument.span());
        return "";
    }

    int number(EnvironmentIntrinsicFactory.ArgumentValue argument, String name) {
        if (argument.value() instanceof EnvironmentValue.Number number) {
            return number.value();
        }
        typeError(name, "integer", argument.span());
        return 0;
    }

    Optional<NamespacedId> id(String raw, SourceSpan span) {
        try {
            return Optional.of(NamespacedId.of(raw));
        } catch (IllegalArgumentException exception) {
            values.add(error("ESE-ENV-SOURCE-014",
                    "Environment source has invalid namespaced id '" + raw + "'.", span, Map.of("id", raw)));
            return Optional.empty();
        }
    }

    boolean count(
            String target,
            List<EnvironmentIntrinsicFactory.ArgumentValue> arguments,
            SourceSpan span,
            int count) {
        return countRange(arguments, span, new CountBounds(target, count, count));
    }

    boolean countRange(
            String target,
            List<EnvironmentIntrinsicFactory.ArgumentValue> arguments,
            SourceSpan span,
            CountRange range) {
        return countRange(arguments, span, new CountBounds(target, range.min(), range.max()));
    }

    EnvironmentValue unknown(String target, SourceSpan span) {
        values.add(error("ESE-ENV-SOURCE-017",
                "Environment source uses unknown intrinsic '" + target + "'.", span, Map.of("intrinsic", target)));
        return new EnvironmentValue.Invalid();
    }

    SourceLocation location(SourceSpan span) {
        return SourceLocation.of(span.start().line(), span.start().column());
    }

    private List<EnvironmentValue> array(EnvironmentValue value, String name, SourceSpan span) {
        if (value instanceof EnvironmentValue.Array array) {
            return array.values();
        }
        typeError(name, "array", span);
        return List.of();
    }

    private BlockPropertyDef propertyValue(EnvironmentValue value, SourceSpan span) {
        if (value instanceof EnvironmentValue.Property property) {
            return property.value();
        }
        typeError("properties", "Property", span);
        return new BlockPropertyDef("invalid", "invalid", location(span));
    }

    private boolean countRange(
            List<EnvironmentIntrinsicFactory.ArgumentValue> arguments,
            SourceSpan span,
            CountBounds bounds) {
        if (arguments.size() >= bounds.min() && arguments.size() <= bounds.max()
                && noNamed(bounds.target(), arguments, span)) {
            return true;
        }
        values.add(error("ESE-ENV-SOURCE-015",
                bounds.target() + " has invalid argument count.", span,
                Map.of("intrinsic", bounds.target(), "arguments", Integer.toString(arguments.size()))));
        return false;
    }

    private boolean noNamed(
            String target,
            List<EnvironmentIntrinsicFactory.ArgumentValue> arguments,
            SourceSpan span) {
        Optional<String> named = arguments.stream().flatMap(argument -> argument.name().stream()).findFirst();
        named.ifPresent(name -> values.add(error("ESE-ENV-SOURCE-016",
                target + " does not accept named argument '" + name + "'.", span,
                Map.of("intrinsic", target, "argument", name))));
        return named.isEmpty();
    }

    private void typeError(String name, String expected, SourceSpan span) {
        values.add(error("ESE-ENV-SOURCE-018",
                "Environment argument '" + name + "' must be " + expected + ".", span,
                Map.of("argument", name, "expected", expected)));
    }

    private StoryDiagnostic error(String code, String message, SourceSpan span, Map<String, String> context) {
        return diagnostics.error(new EnvironmentSourceDiagnostics.Issue(code, message, source, span, context));
    }

    record CountRange(int min, int max) {
    }

    private record CountBounds(String target, int min, int max) {
    }
}
