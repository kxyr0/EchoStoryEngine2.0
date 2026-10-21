package io.github.kxyr0.echostoryengine.story.echoscript.binding;

import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassSectionSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.FieldSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceStory;
import io.github.kxyr0.echostoryengine.story.echoscript.diagnostic.SemanticDiagnosticFactory;
import io.github.kxyr0.echostoryengine.story.echoscript.binding.highlevel.HighLevelStoryResourceBinder;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.BuildMethodBinder;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.LinearMethodBinder;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.NodeMethodBinder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Binds EchoScript compilation units to the single supported StoryResource profile.
 */
public final class StoryResourceBinder {
    private static final String PROFILE = "StoryResource";
    private final SemanticDiagnosticFactory diagnostics = new SemanticDiagnosticFactory();
    private final BuildMethodBinder buildBinder = new BuildMethodBinder(diagnostics);
    private final LinearMethodBinder linearBinder = new LinearMethodBinder(diagnostics);
    private final NodeMethodBinder nodeBinder = new NodeMethodBinder(diagnostics);
    private final HighLevelStoryResourceBinder highLevelBinder = new HighLevelStoryResourceBinder(diagnostics);

    public StoryResourceBinding bind(CompilationUnitSyntax unit, String sourceId, Instant occurredAt) {
        BindingContext binding = BindingContext.source(sourceId, occurredAt);
        ClassSyntax resourceClass = unit.classDeclaration();
        if (!PROFILE.equals(resourceClass.baseType().name().text())) {
            StoryDiagnostic diagnostic = diagnostics.create(
                    "ESE-SOURCE-014",
                    binding,
                    resourceClass.baseType().span(),
                    Map.of("profile", resourceClass.baseType().name().text()));
            return new StoryResourceBinding(Optional.empty(), List.of(diagnostic));
        }
        return highLevel(resourceClass) ? highLevelBinder.bind(resourceClass, binding)
                : bindProfile(resourceClass, binding);
    }

    private boolean highLevel(ClassSyntax resourceClass) {
        return resourceClass.members().stream().anyMatch(member ->
                member instanceof ClassCallSyntax || member instanceof ClassSectionSyntax);
    }

    private StoryResourceBinding bindProfile(ClassSyntax resourceClass, BindingContext binding) {
        List<StoryDiagnostic> values = new ArrayList<>();
        List<MethodSyntax> methods = methods(resourceClass, values, binding);
        Optional<MethodSyntax> build = method(methods, "Build");
        Optional<MethodSyntax> nodes = method(methods, "Nodes");
        Optional<MethodSyntax> onStart = method(methods, "OnStart");
        if (build.isEmpty()) {
            values.add(diagnostics.create("ESE-SOURCE-017", binding, resourceClass.span(), Map.of("method", "Build")));
        }
        if (nodes.isEmpty() && onStart.isEmpty()) {
            values.add(diagnostics.create("ESE-SOURCE-025", binding, resourceClass.span(),
                    Map.of("method", "Nodes or OnStart")));
        }
        if (!values.isEmpty() || build.isEmpty() || (nodes.isEmpty() && onStart.isEmpty())) {
            return new StoryResourceBinding(Optional.empty(), values);
        }
        ProfileMembers members = new ProfileMembers(build.orElseThrow(), nodes, onStart, resourceClass);
        return bindMembers(members, binding, values);
    }

    private StoryResourceBinding bindMembers(
            ProfileMembers members,
            BindingContext binding,
            List<StoryDiagnostic> values) {
        Optional<StoryHeader> header = buildBinder.bind(members.build(), binding, values);
        if (header.isEmpty() || !values.isEmpty()) {
            return new StoryResourceBinding(Optional.empty(), values);
        }
        StoryHeader storyHeader = header.orElseThrow();
        BindingContext storyBinding = binding.story(storyHeader.storyId());
        List<SourceNode> boundNodes = bindNodes(members, storyHeader, storyBinding, values);
        if (!values.isEmpty()) {
            return new StoryResourceBinding(Optional.empty(), values);
        }
        SourceStory story = new SourceStory(
                binding.sourceId(),
                Optional.of(1),
                Optional.of(storyHeader.storyId()),
                Optional.of(storyHeader.startNode()),
                boundNodes,
                Set.of("formatVersion", "storyId", "startNode", "nodes"),
                SourceLocation.of(
                        members.resourceClass().span().start().line(),
                        members.resourceClass().span().start().column()));
        return new StoryResourceBinding(Optional.of(story), values);
    }

    private List<SourceNode> bindNodes(
            ProfileMembers members,
            StoryHeader header,
            BindingContext binding,
            List<StoryDiagnostic> values) {
        return members.nodes()
                .map(nodes -> nodeBinder.bind(nodes, binding, values))
                .orElseGet(() -> linearBinder.bind(members.onStart().orElseThrow(),
                        header.startNode(), binding, values));
    }

    private List<MethodSyntax> methods(
            ClassSyntax resourceClass,
            List<StoryDiagnostic> values,
            BindingContext binding) {
        List<MethodSyntax> methods = new ArrayList<>();
        for (MemberSyntax member : resourceClass.members()) {
            if (member instanceof MethodSyntax method) {
                methods.add(method);
                if (!Set.of("Build", "Nodes", "OnStart").contains(method.name())) {
                    values.add(diagnostics.create("ESE-SOURCE-019", binding, method.span(),
                            Map.of("member", method.name())));
                }
            } else if (member instanceof FieldSyntax field) {
                values.add(diagnostics.create("ESE-SOURCE-020", binding, field.span(),
                        Map.of("type", field.type().name().text(), "member", field.name())));
            }
        }
        return List.copyOf(methods);
    }

    private Optional<MethodSyntax> method(List<MethodSyntax> methods, String name) {
        return methods.stream().filter(method -> method.name().equals(name)).findFirst();
    }

    private record ProfileMembers(
            MethodSyntax build,
            Optional<MethodSyntax> nodes,
            Optional<MethodSyntax> onStart,
            ClassSyntax resourceClass) {
    }
}
