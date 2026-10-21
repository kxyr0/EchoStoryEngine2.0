package io.github.kxyr0.echonpc.echoscript.binding;

import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.echoscript.NpcCompileResult;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Reads ES2 metadata calls without executing user code. */
final class HighLevelNpcBinder {
    private static final Map<String, String> FIELDS = Map.of(
            "Id", "id", "Display", "display", "Skin", "skin",
            "NameColor", "nameColor", "DefaultForm", "defaultForm");

    NpcCompileResult bind(ClassSyntax resource, String sourceId, List<NpcDiagnostic> diagnostics) {
        List<ArgumentSyntax> arguments = new ArrayList<>();
        for (var member : resource.members()) {
            if (!(member instanceof ClassCallSyntax metadata)) {
                diagnostics.add(SemanticDiagnosticFactory.error("ENPC-NPCRESOURCE-MIXED-PROFILE",
                        "ES2 NPC принимает только metadata; Build, поля и блоки с ними не смешиваются.",
                        resource.name(), member.span()));
                continue;
            }
            read(metadata.invocation(), arguments, diagnostics);
        }
        if (arguments.stream().noneMatch(argument -> argument.name().orElse("").equals("id"))) {
            int separator = sourceId.indexOf(':');
            String filename = sourceId.substring(Math.max(sourceId.lastIndexOf('/'), separator) + 1);
            if (separator > 0 && filename.endsWith(".npc.es")) {
                String id = sourceId.substring(0, separator) + ":" + filename.substring(0, filename.length() - 7);
                arguments.add(new ArgumentSyntax(Optional.of("id"),
                        new StringLiteralExpressionSyntax(id, resource.span()), resource.span()));
            }
        }
        var call = new InvocationExpressionSyntax(new NameExpressionSyntax("Npc", resource.span()),
                arguments, resource.span());
        var definition = new NpcInvocationReader().read(call, diagnostics);
        return new NpcCompileResult(definition, diagnostics);
    }

    private void read(InvocationExpressionSyntax call, List<ArgumentSyntax> arguments,
            List<NpcDiagnostic> diagnostics) {
        String name = call.target() instanceof NameExpressionSyntax target ? target.name() : "";
        String field = FIELDS.get(name);
        if (field == null || call.arguments().size() != 1 || call.arguments().get(0).name().isPresent()
                || !(call.arguments().get(0).value() instanceof StringLiteralExpressionSyntax)) {
            diagnostics.add(SemanticDiagnosticFactory.error("ENPC-NPCRESOURCE-METADATA",
                    "Ожидался Id, Display, Skin, NameColor или DefaultForm с одной строкой.", name, call.span()));
            return;
        }
        var value = call.arguments().get(0).value();
        arguments.add(new ArgumentSyntax(Optional.of(field),
                List.of("skin", "nameColor", "defaultForm").contains(field) ? call : value, call.span()));
    }
}
