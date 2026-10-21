package io.github.kxyr0.echoseating.core.binding;

import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnostic;
import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnosticCode;
import io.github.kxyr0.echoseating.api.id.SeatingRevision;
import io.github.kxyr0.echoseating.core.catalog.SeatZoneDefinitionRules;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SeatZoneDefinitionBinder {
    public SeatZoneCompileResult bind(CompilationUnitSyntax unit, SeatingRevision revision) {
        List<SeatingDiagnostic> diagnostics = new ArrayList<>();
        validateBaseType(unit, diagnostics);
        SeatZoneFieldCatalog fields = SeatZoneFieldCatalog.from(unit.classDeclaration().members(), diagnostics);
        Optional<MethodSyntax> init = findMethod(unit.classDeclaration().members(), "Init");
        if (init.isEmpty()) {
            diagnostics.add(new SeatingDiagnostic(SeatingDiagnosticCode.SEATING_ZONE_INVALID,
                    unit.classDeclaration().name(), "SeatZoneResource must declare Init()."));
        }
        Optional<SeatZoneInitValues> values = init.flatMap(method -> SeatZoneInitValues.read(
                method, fields, diagnostics));
        if (!diagnostics.isEmpty() || values.isEmpty()) {
            return new SeatZoneCompileResult(Optional.empty(), diagnostics);
        }
        var definition = values.orElseThrow().toDefinition(revision);
        diagnostics.addAll(SeatZoneDefinitionRules.validate(definition));
        return diagnostics.isEmpty()
                ? new SeatZoneCompileResult(Optional.of(definition), List.of())
                : new SeatZoneCompileResult(Optional.empty(), diagnostics);
    }

    private static void validateBaseType(
            CompilationUnitSyntax unit,
            List<SeatingDiagnostic> diagnostics) {
        if (!unit.classDeclaration().baseType().name().text().equals("SeatZoneResource")) {
            diagnostics.add(new SeatingDiagnostic(SeatingDiagnosticCode.SEATING_ZONE_INVALID,
                    unit.classDeclaration().name(), "Source must contain class ... : SeatZoneResource."));
        }
    }

    private static Optional<MethodSyntax> findMethod(List<MemberSyntax> members, String name) {
        return members.stream()
                .filter(MethodSyntax.class::isInstance)
                .map(MethodSyntax.class::cast)
                .filter(method -> method.name().equals(name))
                .findFirst();
    }
}
