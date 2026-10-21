package io.github.kxyr0.echoseating.core.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnosticCode;
import io.github.kxyr0.echoseating.api.id.SeatingRevision;
import io.github.kxyr0.echoseating.api.zone.UnknownSeatPolicy;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import org.junit.jupiter.api.Test;

class SeatZoneDefinitionBinderTest {
    private final SeatZoneDefinitionBinder binder = new SeatZoneDefinitionBinder();

    @Test
    void bindsInitOnlyBreakfastTableZone() {
        SeatZoneCompileResult result = bind("""
                namespace JDH.Chapter1.Home.Seats;
                class BreakfastTableSeats : SeatZoneResource
                {
                    private SeatZoneId BreakfastTable = "jdh:home/breakfast_table";
                    private SeatSlotId MothersSeat = "jdh:home/breakfast_table/seat_3";
                    private void Init()
                    {
                        Id(BreakfastTable);
                        Dimension("minecraft:overworld");
                        Area(Box(Position(10, 64, 10), Position(14, 66, 14)));
                        Detect(RIDING_ANCHOR);
                        UnknownSeats(ALLOW);
                        Slot(MothersSeat, Position(12, 65, 12));
                        MatchRadius(0.85);
                    }
                }
                """);

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        var zone = result.definition().orElseThrow();
        assertEquals("jdh:home/breakfast_table", zone.id().value());
        assertEquals("minecraft:overworld", zone.dimension().value());
        assertEquals(1, zone.slots().size());
        assertEquals(UnknownSeatPolicy.ALLOW, zone.unknownSeatPolicy());
        assertEquals(0.85D, zone.matchRadius(), 0.0001D);
    }

    @Test
    void rejectsMissingDimension() {
        SeatZoneCompileResult result = bind("""
                class MissingDimensionSeats : SeatZoneResource
                {
                    private void Init()
                    {
                        Id("jdh:home/table");
                        Area(Box(Position(0, 0, 0), Position(1, 1, 1)));
                    }
                }
                """);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code()
                        == SeatingDiagnosticCode.SEATING_ZONE_DIMENSION_MISSING));
    }

    private SeatZoneCompileResult bind(String source) {
        ParseResult parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(
                new SourceId("test.es"), source));
        assertTrue(parsed.successful(), () -> parsed.diagnostics().toString());
        return binder.bind(parsed.syntaxTree().orElseThrow(), SeatingRevision.initial());
    }
}
