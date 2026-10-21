package io.github.kxyr0.echothoughtspace.forge.client.style;

import io.github.kxyr0.echothoughtspace.core.combination.ThoughtCombinationRecipe;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeSnapshot;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class ThoughtSpaceConfigCompiler {
    private final ThoughtSpaceStyleBinder styles = new ThoughtSpaceStyleBinder();
    private final ThoughtCombinationBinder combinations = new ThoughtCombinationBinder();
    private final ThoughtDeclarationBinder thoughts = new ThoughtDeclarationBinder();

    Document compile(String sourceId, String source, ThoughtSpaceStyleConfig fallback,
            String namespace, String resourcePath) {
        var parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(new SourceId(sourceId), source));
        if (!parsed.successful()) {
            String diagnostic = parsed.diagnostics().isEmpty() ? "unknown syntax error"
                    : parsed.diagnostics().get(0).code() + ": " + parsed.diagnostics().get(0).message();
            throw new IllegalArgumentException("EchoScript rejected " + sourceId + ": " + diagnostic);
        }
        List<ClassSyntax> resources = parsed.syntaxTree().orElseThrow().classDeclarations().stream()
                .filter(type -> type.baseType().name().text().equals("ThoughtSpaceResource")).toList();
        if (resources.size() != 1) {
            throw new IllegalArgumentException(sourceId + " must declare exactly one ThoughtSpaceResource.");
        }
        ClassSyntax resource = resources.get(0);
        validateMembers(resource);
        Optional<ThoughtSpaceStyleConfig> style = styles.bind(resource, fallback);
        List<ThoughtCombinationRecipe> recipes = combinations.bind(resource, namespace, resourcePath);
        List<ThoughtNodeSnapshot> declaredThoughts = thoughts.bind(resource, namespace, resourcePath);
        if (style.isEmpty() && recipes.isEmpty() && declaredThoughts.isEmpty()) {
            throw new IllegalArgumentException("ThoughtSpaceResource contains no Style, Thought or Combination.");
        }
        return new Document(style, recipes, declaredThoughts);
    }

    private static void validateMembers(ClassSyntax resource) {
        for (var member : resource.members()) {
            if (member instanceof ClassCallSyntax call
                    && ThoughtScriptCallReader.name(call.invocation()).equals("Style")) continue;
            if (member instanceof MethodSyntax method
                    && List.of("Thought", "Combination").contains(method.returnType().name().text())) continue;
            throw new IllegalArgumentException("Unsupported ThoughtSpaceResource member.");
        }
    }

    record Document(Optional<ThoughtSpaceStyleConfig> style, List<ThoughtCombinationRecipe> recipes,
            List<ThoughtNodeSnapshot> thoughts) {
        Document {
            style = Objects.requireNonNull(style, "style");
            recipes = List.copyOf(recipes);
            thoughts = List.copyOf(thoughts);
        }
    }
}
