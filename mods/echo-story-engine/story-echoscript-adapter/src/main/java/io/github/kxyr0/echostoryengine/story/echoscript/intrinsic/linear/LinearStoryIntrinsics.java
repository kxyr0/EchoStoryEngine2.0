package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.linear;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.IntrinsicCallReader;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.NodeSpec;
import java.util.Optional;
import java.util.Set;

/** Binds ES2 linear Story-owned commands and generic adapter actions. */
public final class LinearStoryIntrinsics {
    public static final Set<String> TARGETS = Set.of("Story.Set", "Story.Require", "Action");

    public Optional<LinearNodeChain.Entry> bind(IntrinsicCallReader reader, SourceSpan span) {
        return switch (reader.targetName()) {
            case "Story.Set" -> storySet(reader, span);
            case "Story.Require" -> storyRequire(reader, span);
            case "Action" -> genericAction(reader, span);
            default -> Optional.empty();
        };
    }

    private Optional<LinearNodeChain.Entry> storySet(IntrinsicCallReader reader, SourceSpan span) {
        reader.requirePositionalCount("Story.Set", 2);
        reader.rejectUnknownNamed("Story.Set", Set.of());
        Optional<String> variable = reader.positionalScalar(0, "Story.Set", "variable");
        Optional<String> value = reader.positionalScalar(1, "Story.Set", "value");
        return Optional.of(new LinearNodeChain.Entry(
                "set_variable", NodeSpec.set(span, Optional.empty(), variable, value)));
    }

    private Optional<LinearNodeChain.Entry> storyRequire(IntrinsicCallReader reader, SourceSpan span) {
        reader.requirePositionalCount("Story.Require", 2);
        reader.rejectUnknownNamed("Story.Require", Set.of());
        Optional<String> variable = reader.positionalScalar(0, "Story.Require", "variable");
        Optional<String> value = reader.positionalScalar(1, "Story.Require", "value");
        return Optional.of(new LinearNodeChain.Entry(
                "require_variable", NodeSpec.require(span, Optional.empty(), variable, value)));
    }

    private Optional<LinearNodeChain.Entry> genericAction(IntrinsicCallReader reader, SourceSpan span) {
        reader.requirePositionalCount("Action", 0);
        return Optional.of(new LinearNodeChain.Entry("action", NodeSpec.action(span, readerNamedFields(reader))));
    }

    private NodeSpec.ActionFields readerNamedFields(IntrinsicCallReader reader) {
        Set<String> reserved = Set.of("type", "output");
        return new NodeSpec.ActionFields(
                Optional.empty(),
                reader.requiredNamedString("type", "Action"),
                reader.optionalNamedString("output", "Action"),
                reader.additionalNamedScalars("Action", reserved));
    }
}
