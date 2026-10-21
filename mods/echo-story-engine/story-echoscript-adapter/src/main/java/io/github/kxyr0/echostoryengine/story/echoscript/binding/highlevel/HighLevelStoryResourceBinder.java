package io.github.kxyr0.echostoryengine.story.echoscript.binding.highlevel;

import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassSectionSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceStory;
import io.github.kxyr0.echostoryengine.story.echoscript.binding.BindingContext;
import io.github.kxyr0.echostoryengine.story.echoscript.binding.StoryResourceBinding;
import io.github.kxyr0.echostoryengine.story.echoscript.diagnostic.SemanticDiagnosticFactory;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.IntrinsicCallReader;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.LinearMethodBinder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Lowers ES2 class-level Story intent to the strict source graph. */
public final class HighLevelStoryResourceBinder {
    private static final String START_NODE = "start";
    private final SemanticDiagnosticFactory diagnostics;
    private final LinearMethodBinder linearBinder;
    private final StorySourceContextResolver contexts = new StorySourceContextResolver();

    public HighLevelStoryResourceBinder(SemanticDiagnosticFactory diagnostics) {
        this.diagnostics = diagnostics;
        linearBinder = new LinearMethodBinder(diagnostics);
    }

    public StoryResourceBinding bind(ClassSyntax resource, BindingContext binding) {
        List<StoryDiagnostic> values = new ArrayList<>();
        List<ClassSectionSyntax> starts = sections(resource, "OnStart");
        List<ClassCallSyntax> ids = calls(resource, "Id");
        validateMembers(resource, values, binding);
        validateStartCount(starts, resource, values, binding);
        if (ids.size() > 1) {
            values.add(unsupported(ids.get(1), "duplicate Id", binding));
        }
        Optional<String> storyId = storyId(ids, binding, resource, values);
        if (!values.isEmpty() || starts.size() != 1 || storyId.isEmpty()) {
            return new StoryResourceBinding(Optional.empty(), values);
        }
        ClassSectionSyntax start = starts.get(0);
        validateEmptyHeader(start.invocation(), "OnStart", binding, values);
        BindingContext storyBinding = binding.story(storyId.orElseThrow());
        List<SourceNode> nodes = linearBinder.bind(start, START_NODE, storyBinding, values);
        if (!values.isEmpty()) {
            return new StoryResourceBinding(Optional.empty(), values);
        }
        SourceStory story = new SourceStory(binding.sourceId(), Optional.of(1), storyId,
                Optional.of(START_NODE), nodes, Set.of("formatVersion", "storyId", "startNode", "nodes"),
                SourceLocation.of(resource.span().start().line(), resource.span().start().column()));
        return new StoryResourceBinding(Optional.of(story), values);
    }

    private Optional<String> storyId(List<ClassCallSyntax> ids, BindingContext binding, ClassSyntax resource,
            List<StoryDiagnostic> values) {
        if (ids.isEmpty()) {
            Optional<String> inferred = contexts.resolveStoryId(binding.sourceId());
            if (inferred.isEmpty()) {
                values.add(diagnostics.create("ESE-SOURCE-004", binding, resource.span(),
                        Map.of("storyId", "<inferred>", "reason", "canonical source path required")));
            }
            return inferred;
        }
        InvocationExpressionSyntax call = ids.get(0).invocation();
        IntrinsicCallReader reader = new IntrinsicCallReader(call, diagnostics, binding);
        reader.requirePositionalCount("Id", 1);
        reader.rejectUnknownNamed("Id", Set.of());
        Optional<String> value = reader.positionalString(0, "Id", "storyId");
        values.addAll(reader.diagnostics());
        return reader.hasErrors() ? Optional.empty() : value;
    }

    private void validateMembers(ClassSyntax resource, List<StoryDiagnostic> values, BindingContext binding) {
        for (MemberSyntax member : resource.members()) {
            boolean supported = member instanceof ClassCallSyntax call && name(call.invocation()).equals("Id")
                    || member instanceof ClassSectionSyntax section
                    && Set.of("OnStart", "When").contains(name(section.invocation()));
            if (!supported) {
                values.add(unsupported(member, member.getClass().getSimpleName(), binding));
            }
        }
    }

    private void validateStartCount(List<ClassSectionSyntax> starts, ClassSyntax resource,
            List<StoryDiagnostic> values, BindingContext binding) {
        if (starts.isEmpty()) {
            values.add(diagnostics.create("ESE-SOURCE-025", binding, resource.span(), Map.of("method", "OnStart")));
        } else if (starts.size() > 1) {
            values.add(unsupported(starts.get(1), "duplicate OnStart", binding));
        }
    }

    private void validateEmptyHeader(InvocationExpressionSyntax call, String name, BindingContext binding,
            List<StoryDiagnostic> values) {
        IntrinsicCallReader reader = new IntrinsicCallReader(call, diagnostics, binding);
        reader.requirePositionalCount(name, 0);
        reader.rejectUnknownNamed(name, Set.of());
        values.addAll(reader.diagnostics());
    }

    private List<ClassCallSyntax> calls(ClassSyntax resource, String target) {
        return resource.members().stream().filter(ClassCallSyntax.class::isInstance).map(ClassCallSyntax.class::cast)
                .filter(call -> name(call.invocation()).equals(target)).toList();
    }

    private List<ClassSectionSyntax> sections(ClassSyntax resource, String target) {
        return resource.members().stream().filter(ClassSectionSyntax.class::isInstance)
                .map(ClassSectionSyntax.class::cast).filter(section -> name(section.invocation()).equals(target))
                .toList();
    }

    private String name(InvocationExpressionSyntax call) {
        if (call.target() instanceof NameExpressionSyntax name) {
            return name.name();
        }
        if (call.target() instanceof MemberAccessExpressionSyntax access
                && access.target() instanceof NameExpressionSyntax qualifier) {
            return qualifier.name() + "." + access.memberName();
        }
        return "";
    }

    private StoryDiagnostic unsupported(MemberSyntax member, String name, BindingContext binding) {
        return diagnostics.create("ESE-SOURCE-019", binding, member.span(), Map.of("member", name));
    }
}
