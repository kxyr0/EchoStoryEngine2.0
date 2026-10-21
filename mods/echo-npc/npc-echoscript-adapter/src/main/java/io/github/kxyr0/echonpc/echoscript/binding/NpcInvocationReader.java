package io.github.kxyr0.echonpc.echoscript.binding;

import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.appearance.NpcNameColor;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import io.github.kxyr0.echonpc.core.definition.NpcDefinition;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class NpcInvocationReader {
    private final NpcArgumentReader arguments = new NpcArgumentReader();

    Optional<NpcDefinition> read(InvocationExpressionSyntax call, List<NpcDiagnostic> diagnostics) {
        Map<String, io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax> named =
                arguments.read(call.arguments(), diagnostics);
        String idText = arguments.stringValue("id", named, diagnostics);
        String display = arguments.stringValue("display", named, diagnostics);
        String skinText = arguments.skinValue(named, diagnostics);
        String defaultFormText = arguments.defaultFormValue(named, diagnostics);
        Optional<NpcNameColor> nameColor = arguments.nameColorValue(named, diagnostics);
        Optional<NpcDefinitionId> id = definitionId(idText, call, diagnostics);
        Optional<NpcSkinId> skin = skinId(skinText, call, diagnostics);
        Optional<NpcFormId> defaultForm = formId(defaultFormText, call, diagnostics);
        if (display.isBlank()) {
            diagnostics.add(SemanticDiagnosticFactory.error("ENPC-NPCRESOURCE-EMPTY-DISPLAY",
                    "display не должен быть пустым.", "display", call.span()));
        }
        if (diagnostics.stream().anyMatch(NpcDiagnostic::error) || id.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(NpcDefinition.humanoid(id.orElseThrow(), display, nameColor, skin, defaultForm));
    }

    private Optional<NpcDefinitionId> definitionId(
            String value,
            InvocationExpressionSyntax call,
            List<NpcDiagnostic> diagnostics
    ) {
        try {
            return value.isBlank() ? Optional.empty() : Optional.of(new NpcDefinitionId(value));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(SemanticDiagnosticFactory.error("ENPC-NPCRESOURCE-ID-INVALID",
                    "NpcDefinitionId имеет неверный формат.", value, call.span()));
            return Optional.empty();
        }
    }

    private Optional<NpcSkinId> skinId(
            String value,
            InvocationExpressionSyntax call,
            List<NpcDiagnostic> diagnostics
    ) {
        try {
            return value.isBlank() ? Optional.empty() : Optional.of(new NpcSkinId(value));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(SemanticDiagnosticFactory.error("ENPC-NPCRESOURCE-SKIN-INVALID",
                    "NpcSkinId имеет неверный формат.", value, call.span()));
            return Optional.empty();
        }
    }

    private Optional<NpcFormId> formId(
            String value,
            InvocationExpressionSyntax call,
            List<NpcDiagnostic> diagnostics
    ) {
        try {
            return value.isBlank() ? Optional.empty() : Optional.of(new NpcFormId(value));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(SemanticDiagnosticFactory.error("ENPC-NPCRESOURCE-DEFAULT-FORM-INVALID",
                    "NpcFormId имеет неверный формат.", value, call.span()));
            return Optional.empty();
        }
    }
}
