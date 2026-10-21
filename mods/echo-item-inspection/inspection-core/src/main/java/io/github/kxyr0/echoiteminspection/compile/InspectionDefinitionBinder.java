package io.github.kxyr0.echoiteminspection.compile;

import io.github.kxyr0.echoiteminspection.model.ItemInspectionDefinition;
import io.github.kxyr0.echoiteminspection.model.ItemInspectionDefinitionRevision;
import io.github.kxyr0.echoiteminspection.model.ItemInspectionDescriptor;
import io.github.kxyr0.echoiteminspection.model.ItemInspectionText;
import io.github.kxyr0.echoiteminspection.model.ItemInspectionView;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassSectionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Resource semantics live in this consumer, while EchoScript stays a syntax-only library. */
final class InspectionDefinitionBinder {
    ItemInspectionDefinition bind(CompilationUnitSyntax unit, InspectionSource source,
            ItemInspectionDefinitionRevision revision) {
        if (unit.classDeclarations().size() != 1) {
            throw new BindingFailure("ITEM_INSPECTION_INVALID_RESOURCE", "Exactly one ItemInspectionResource is required.", unit.span());
        }
        ClassSyntax resource = unit.classDeclaration();
        if (!resource.baseType().name().text().equals("ItemInspectionResource") || resource.baseType().arrayRank() != 0) {
            throw new BindingFailure("ITEM_INSPECTION_INVALID_RESOURCE", "Expected ItemInspectionResource base type.", resource.span());
        }
        Declarations declarations = declarations(resource);
        ClassCallSyntax item = declarations.item();
        String itemId = InspectionArguments.item(item.invocation());
        ItemInspectionView settings = declarations.view() == null ? ItemInspectionView.defaults()
                : new InspectionViewBinder().bind(declarations.view(), source);
        var text = new ItemInspectionText(InspectionArguments.text(invocation(declarations.title()), ItemInspectionText.MAX_TITLE_LENGTH),
                InspectionArguments.text(invocation(declarations.description()), ItemInspectionText.MAX_DESCRIPTION_LENGTH));
        var id = source.inferredId();
        var descriptor = new ItemInspectionDescriptor(id, itemId, settings, resource.span(),
                item.invocation().arguments().get(0).span(), text);
        return new ItemInspectionDefinition(id, itemId, revision, settings, descriptor, text);
    }

    private Declarations declarations(ClassSyntax resource) {
        List<ClassCallSyntax> calls = new ArrayList<>();
        ClassSectionSyntax view = null;
        for (var member : resource.members()) {
            if (member instanceof ClassCallSyntax call && Set.of("Item", "Title", "Description")
                    .contains(InspectionArguments.name(call.invocation()))) {
                calls.add(call);
            } else if (member instanceof ClassSectionSyntax section
                    && InspectionArguments.name(section.invocation()).equals("View")) {
                if (view != null) throw InspectionArguments.failure("View may be declared only once.", section.invocation());
                view = section;
            } else {
                throw new BindingFailure("ITEM_INSPECTION_INVALID_DECLARATION",
                        "Only Item and optional Title, Description, and View are supported.", member.span());
            }
        }
        ClassCallSyntax item = call(calls, "Item");
        if (item == null) {
            throw new BindingFailure("ITEM_INSPECTION_ITEM_REQUIRED", "Item(...) is required.", resource.span());
        }
        return new Declarations(item, view, call(calls, "Title"), call(calls, "Description"));
    }

    private ClassCallSyntax call(List<ClassCallSyntax> calls, String name) {
        var matches = calls.stream().filter(value -> InspectionArguments.name(value.invocation()).equals(name)).toList();
        if (matches.size() > 1) throw InspectionArguments.failure(name + " may be declared only once.", matches.get(1).invocation());
        return matches.isEmpty() ? null : matches.get(0);
    }

    private InvocationExpressionSyntax invocation(ClassCallSyntax call) {
        return call == null ? null : call.invocation();
    }

    private record Declarations(ClassCallSyntax item, ClassSectionSyntax view,
            ClassCallSyntax title, ClassCallSyntax description) { }
}
