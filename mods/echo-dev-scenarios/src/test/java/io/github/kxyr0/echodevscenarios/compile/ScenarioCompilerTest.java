package io.github.kxyr0.echodevscenarios.compile;

import io.github.kxyr0.echodevscenarios.api.ScenarioTarget;
import io.github.kxyr0.echodevscenarios.compile.source.ScenarioSourceInput;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioCompilerTest {
    @Test
    void compilesChemistryScenarioAndInfersTreeIdentity() {
        ScenarioCompiler compiler = new ScenarioCompiler(catalog(true));
        ScenarioCompileResult result = compiler.compile(new ScenarioSourceInput(
                "dev/scenarios/chapter1/day2/chemistry.es", """
                class ChemistryClass : DevScenarioResource
                {
                    Given()
                    {
                        Time.Set(Day(2), "09:55");
                        Player.Place(UniversityHall);
                        Npc.Ensure(Nolan);
                        Npc.Ensure(Chris);
                        Npc.Ensure(ProfessorHalden);
                        Observation.Activate("jdh:university_day2");
                    }
                    Launch("jdh:chemistry_class");
                }
                """));

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        assertEquals("chapter1.day2.chemistry", result.definition().orElseThrow().id().value());
        assertEquals(List.of("chapter1", "day2"), result.definition().orElseThrow().groups());
        assertEquals("Chemistry Class", result.definition().orElseThrow().title());
        assertEquals(6, result.definition().orElseThrow().setup().size());
        assertEquals(ScenarioTarget.Kind.SCENE, result.definition().orElseThrow().target().kind());
    }

    @Test
    void brokenTargetDisablesScenarioAtCompileBoundary() {
        ScenarioCompileResult result = new ScenarioCompiler(catalog(false)).compile(new ScenarioSourceInput(
                "dev/scenarios/broken.es", "class Broken : DevScenarioResource { Launch(\"jdh:missing\"); }"));

        assertTrue(result.diagnostics().stream().anyMatch(value -> value.code().equals("DEV_SCENARIO_INVALID")));
    }

    private ScenarioCatalog catalog(boolean targetExists) {
        List<ScenarioSymbol> symbols = List.of(
                new ScenarioSymbol("UniversityHall", "jdh:university_hall", ScenarioSymbolKind.LOCATION),
                new ScenarioSymbol("Nolan", "jdh:nolan", ScenarioSymbolKind.NPC),
                new ScenarioSymbol("Chris", "jdh:chris", ScenarioSymbolKind.NPC),
                new ScenarioSymbol("ProfessorHalden", "jdh:professor_halden", ScenarioSymbolKind.NPC),
                new ScenarioSymbol("UniversityObservation", "jdh:university_day2", ScenarioSymbolKind.OBSERVATION));
        return new ScenarioCatalog() {
            public List<ScenarioSymbol> symbols() { return symbols; }
            public List<ScenarioTarget> targets(String id) {
                return targetExists && id.equals("jdh:chemistry_class")
                        ? List.of(new ScenarioTarget(ScenarioTarget.Kind.SCENE, id)) : List.of();
            }
        };
    }
}
