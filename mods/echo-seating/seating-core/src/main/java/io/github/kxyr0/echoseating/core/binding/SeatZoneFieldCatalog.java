package io.github.kxyr0.echoseating.core.binding;

import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnostic;
import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnosticCode;
import io.github.kxyr0.echoscript.api.syntax.declaration.FieldSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class SeatZoneFieldCatalog {
    private final Map<String, String> strings;

    private SeatZoneFieldCatalog(Map<String, String> strings) {
        this.strings = Map.copyOf(strings);
    }

    static SeatZoneFieldCatalog from(List<MemberSyntax> members, List<SeatingDiagnostic> diagnostics) {
        Map<String, String> values = new LinkedHashMap<>();
        members.stream()
                .filter(FieldSyntax.class::isInstance)
                .map(FieldSyntax.class::cast)
                .forEach(field -> read(field, values, diagnostics));
        return new SeatZoneFieldCatalog(values);
    }

    Optional<String> string(String name) {
        return Optional.ofNullable(strings.get(name));
    }

    private static void read(
            FieldSyntax field,
            Map<String, String> values,
            List<SeatingDiagnostic> diagnostics) {
        String type = field.type().name().text();
        if (!type.equals("SeatZoneId") && !type.equals("SeatSlotId")) {
            return;
        }
        if (field.initializer().orElse(null) instanceof StringLiteralExpressionSyntax literal) {
            values.put(field.name(), literal.value());
        } else {
            diagnostics.add(new SeatingDiagnostic(SeatingDiagnosticCode.SEATING_ZONE_INVALID,
                    field.name(), "SeatZoneId and SeatSlotId fields require string initializers."));
        }
    }
}
