package io.github.kxyr0.echoseating.core.binding;

import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnostic;
import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnosticCode;
import io.github.kxyr0.echoseating.api.id.SeatSlotId;
import io.github.kxyr0.echoseating.api.id.SeatZoneId;
import io.github.kxyr0.echoseating.api.id.SeatingRevision;
import io.github.kxyr0.echoseating.api.world.BlockPosition;
import io.github.kxyr0.echoseating.api.world.DimensionId;
import io.github.kxyr0.echoseating.api.world.Vector3;
import io.github.kxyr0.echoseating.api.zone.SeatDetectionPolicy;
import io.github.kxyr0.echoseating.api.zone.SeatSlotDefinition;
import io.github.kxyr0.echoseating.api.zone.SeatZoneBox;
import io.github.kxyr0.echoseating.api.zone.SeatZoneDefinition;
import io.github.kxyr0.echoseating.api.zone.UnknownSeatPolicy;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class SeatZoneInitValues {
    private Optional<SeatZoneId> id = Optional.empty();
    private Optional<DimensionId> dimension = Optional.empty();
    private Optional<SeatZoneBox> area = Optional.empty();
    private SeatDetectionPolicy detection = SeatDetectionPolicy.RIDING_ANCHOR;
    private UnknownSeatPolicy unknown = UnknownSeatPolicy.ALLOW;
    private double matchRadius = 0.85D;
    private final List<SeatSlotDefinition> slots = new ArrayList<>();

    static Optional<SeatZoneInitValues> read(
            MethodSyntax init,
            SeatZoneFieldCatalog fields,
            List<SeatingDiagnostic> diagnostics) {
        SeatZoneInitValues values = new SeatZoneInitValues();
        init.body().statements().stream()
                .flatMap(statement -> SeatingCallSyntax.statementCall(statement).stream())
                .forEach(call -> values.readCall(call, fields, diagnostics));
        values.validateRequired(diagnostics);
        return diagnostics.isEmpty() ? Optional.of(values) : Optional.empty();
    }

    SeatZoneDefinition toDefinition(SeatingRevision revision) {
        return new SeatZoneDefinition(id.orElseThrow(), dimension.orElseThrow(), area.orElseThrow(),
                detection, unknown, slots, matchRadius, revision);
    }

    private void readCall(
            InvocationExpressionSyntax call,
            SeatZoneFieldCatalog fields,
            List<SeatingDiagnostic> diagnostics) {
        switch (SeatingCallSyntax.name(call)) {
            case "Id" -> resolveId(call, fields, diagnostics)
                    .ifPresent(value -> id = Optional.of(new SeatZoneId(value)));
            case "Dimension" -> SeatingCallSyntax.positionalString(call, 0)
                    .ifPresent(value -> dimension = Optional.of(new DimensionId(value)));
            case "Area" -> readArea(call, diagnostics).ifPresent(value -> area = Optional.of(value));
            case "Detect" -> readDetection(call, diagnostics).ifPresent(value -> detection = value);
            case "UnknownSeats" -> readUnknown(call, diagnostics).ifPresent(value -> unknown = value);
            case "Slot" -> readSlot(call, fields, diagnostics).ifPresent(slots::add);
            case "MatchRadius" -> SeatingCallSyntax.positionalDouble(call, 0)
                    .ifPresent(value -> matchRadius = value);
            default -> diagnostics.add(invalid(SeatingCallSyntax.name(call), "Unsupported SeatZone Init call."));
        }
    }

    private Optional<SeatZoneBox> readArea(
            InvocationExpressionSyntax areaCall,
            List<SeatingDiagnostic> diagnostics) {
        Optional<InvocationExpressionSyntax> box = SeatingCallSyntax.positionalInvocation(areaCall, 0)
                .filter(call -> SeatingCallSyntax.name(call).equals("Box"));
        if (box.isEmpty()) {
            diagnostics.add(invalid("Area", "Area requires Box(Position(min), Position(max))."));
            return Optional.empty();
        }
        Optional<BlockPosition> min = positionBlock(box.orElseThrow(), 0, diagnostics);
        Optional<BlockPosition> max = positionBlock(box.orElseThrow(), 1, diagnostics);
        return min.flatMap(a -> max.map(b -> new SeatZoneBox(a, b)));
    }

    private Optional<SeatSlotDefinition> readSlot(
            InvocationExpressionSyntax call,
            SeatZoneFieldCatalog fields,
            List<SeatingDiagnostic> diagnostics) {
        Optional<String> slotId = resolveText(call, 0, fields);
        Optional<Vector3> anchor = position(call, 1, diagnostics);
        if (slotId.isEmpty() || anchor.isEmpty()) {
            diagnostics.add(invalid("Slot", "Slot requires SeatSlotId and Position(x, y, z)."));
            return Optional.empty();
        }
        return Optional.of(new SeatSlotDefinition(new SeatSlotId(slotId.orElseThrow()), anchor.orElseThrow()));
    }

    private Optional<BlockPosition> positionBlock(
            InvocationExpressionSyntax call,
            int index,
            List<SeatingDiagnostic> diagnostics) {
        return position(call, index, diagnostics).map(BlockPosition::containing);
    }

    private Optional<Vector3> position(
            InvocationExpressionSyntax call,
            int index,
            List<SeatingDiagnostic> diagnostics) {
        Optional<InvocationExpressionSyntax> position = SeatingCallSyntax.positionalInvocation(call, index)
                .filter(value -> SeatingCallSyntax.name(value).equals("Position"));
        if (position.isEmpty()) {
            diagnostics.add(invalid("Position", "Expected Position(x, y, z)."));
            return Optional.empty();
        }
        Optional<Double> x = SeatingCallSyntax.positionalDouble(position.orElseThrow(), 0);
        Optional<Double> y = SeatingCallSyntax.positionalDouble(position.orElseThrow(), 1);
        Optional<Double> z = SeatingCallSyntax.positionalDouble(position.orElseThrow(), 2);
        return x.flatMap(a -> y.flatMap(b -> z.map(c -> new Vector3(a, b, c))));
    }

    private Optional<String> resolveId(
            InvocationExpressionSyntax call,
            SeatZoneFieldCatalog fields,
            List<SeatingDiagnostic> diagnostics) {
        Optional<String> value = resolveText(call, 0, fields);
        if (value.isEmpty()) {
            diagnostics.add(invalid("Id", "Id requires SeatZoneId field or namespace:path string."));
        }
        return value;
    }

    private Optional<String> resolveText(InvocationExpressionSyntax call, int index, SeatZoneFieldCatalog fields) {
        return SeatingCallSyntax.positionalString(call, index)
                .or(() -> SeatingCallSyntax.positionalName(call, index).flatMap(fields::string));
    }

    private Optional<SeatDetectionPolicy> readDetection(
            InvocationExpressionSyntax call,
            List<SeatingDiagnostic> diagnostics) {
        return enumValue(call, SeatDetectionPolicy.class, diagnostics, "Detect");
    }

    private Optional<UnknownSeatPolicy> readUnknown(
            InvocationExpressionSyntax call,
            List<SeatingDiagnostic> diagnostics) {
        return enumValue(call, UnknownSeatPolicy.class, diagnostics, "UnknownSeats");
    }

    private <T extends Enum<T>> Optional<T> enumValue(
            InvocationExpressionSyntax call,
            Class<T> type,
            List<SeatingDiagnostic> diagnostics,
            String subject) {
        Optional<String> value = SeatingCallSyntax.positionalText(call, 0);
        if (value.isEmpty()) {
            diagnostics.add(invalid(subject, subject + " requires typed enum value."));
            return Optional.empty();
        }
        try {
            return Optional.of(Enum.valueOf(type, value.orElseThrow()));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(invalid(subject, "Unsupported enum value: " + value.orElseThrow()));
            return Optional.empty();
        }
    }

    private void validateRequired(List<SeatingDiagnostic> diagnostics) {
        if (id.isEmpty()) {
            diagnostics.add(invalid("Id", "SeatZoneResource requires Id(...)."));
        }
        if (dimension.isEmpty()) {
            diagnostics.add(new SeatingDiagnostic(SeatingDiagnosticCode.SEATING_ZONE_DIMENSION_MISSING,
                    "Dimension", "SeatZoneResource requires exact Dimension(\"namespace:path\")."));
        }
        if (area.isEmpty()) {
            diagnostics.add(new SeatingDiagnostic(SeatingDiagnosticCode.SEATING_ZONE_BOUNDS_INVALID,
                    "Area", "SeatZoneResource requires Area(Box(...))."));
        }
    }

    private SeatingDiagnostic invalid(String subject, String message) {
        return new SeatingDiagnostic(SeatingDiagnosticCode.SEATING_ZONE_INVALID, subject, message);
    }
}
