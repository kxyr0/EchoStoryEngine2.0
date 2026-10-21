package io.github.kxyr0.echostoryengine.story.forge.environment.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.BlockPropertyDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.ClearBlockChangeDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentChangeDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentPosition;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentResourceDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentStateDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.SetBlockChangeDef;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation;
import io.github.kxyr0.echostoryengine.story.forge.environment.catalog.EnvironmentCatalogEntry;
import io.github.kxyr0.echostoryengine.story.forge.environment.catalog.EnvironmentCatalogState;
import io.github.kxyr0.echostoryengine.story.forge.environment.resource.EnvironmentResourceCandidate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class EnvironmentServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-31T08:00:00Z");
    private static final NamespacedId ENVIRONMENT_ID = NamespacedId.of("jdh:home/kitchen_table");
    private static final NamespacedId DIMENSION_ID = NamespacedId.of("minecraft:overworld");
    private static final EnvironmentPosition PLATE = new EnvironmentPosition(10, 64, -5);

    @Test
    void unknownStateFailsBeforeChangingWorld() {
        Fixture fixture = fixture();

        assertThrows(EnvironmentApplyException.class,
                () -> fixture.service().set(ENVIRONMENT_ID.asString(), "breakfast", fixture::world));
        assertEquals(Map.of(), fixture.world().placed());
    }

    @Test
    void unknownBlockFailsBeforeChangingWorld() {
        Fixture fixture = fixtureWithBreakfastBlock(NamespacedId.of("displaydelight:missing"));

        assertThrows(EnvironmentApplyException.class,
                () -> fixture.service().set(ENVIRONMENT_ID.asString(), "morning_breakfast", fixture::world));
        assertEquals(Map.of(), fixture.world().placed());
    }

    @Test
    void unknownPropertyFailsBeforeChangingWorld() {
        Fixture fixture = fixtureWithProperty(new BlockPropertyDef("facin", "south", SourceLocation.unknown()));

        assertThrows(EnvironmentApplyException.class,
                () -> fixture.service().set(ENVIRONMENT_ID.asString(), "morning_breakfast", fixture::world));
        assertEquals(Map.of(), fixture.world().placed());
    }

    @Test
    void invalidPropertyValueFailsBeforeChangingWorld() {
        Fixture fixture = fixtureWithProperty(new BlockPropertyDef("facing", "upside_down",
                SourceLocation.unknown()));

        assertThrows(EnvironmentApplyException.class,
                () -> fixture.service().set(ENVIRONMENT_ID.asString(), "morning_breakfast", fixture::world));
        assertEquals(Map.of(), fixture.world().placed());
    }

    @Test
    void repeatedStateApplicationIsIdempotent() throws EnvironmentApplyException {
        Fixture fixture = fixture();

        fixture.service().set(ENVIRONMENT_ID.asString(), "morning_breakfast", fixture::world);
        fixture.service().set(ENVIRONMENT_ID.asString(), "morning_breakfast", fixture::world);

        assertEquals("displaydelight:bacon_and_eggs[facing=south]", fixture.world().placed().get(PLATE));
        assertEquals(1, fixture.world().placed().size());
    }

    @Test
    void nightMorningAfterBreakfastTransitionsReplaceBlocks() throws EnvironmentApplyException {
        Fixture fixture = fixture();

        fixture.service().set(ENVIRONMENT_ID.asString(), "night", fixture::world);
        assertEquals("minecraft:air", fixture.world().placed().get(PLATE));

        fixture.service().set(ENVIRONMENT_ID.asString(), "morning_breakfast", fixture::world);
        assertEquals("displaydelight:bacon_and_eggs[facing=south]", fixture.world().placed().get(PLATE));

        fixture.service().set(ENVIRONMENT_ID.asString(), "after_breakfast", fixture::world);
        assertEquals("minecraft:air", fixture.world().placed().get(PLATE));
    }

    private static Fixture fixture() {
        return fixtureWithProperty(new BlockPropertyDef("facing", "south", SourceLocation.unknown()));
    }

    private static Fixture fixtureWithBreakfastBlock(NamespacedId blockId) {
        return fixtureWithChange(new SetBlockChangeDef(PLATE, blockId, List.of(), Optional.empty(),
                SourceLocation.unknown()));
    }

    private static Fixture fixtureWithProperty(BlockPropertyDef property) {
        return fixtureWithChange(new SetBlockChangeDef(PLATE, NamespacedId.of("displaydelight:bacon_and_eggs"),
                List.of(property), Optional.empty(), SourceLocation.unknown()));
    }

    private static Fixture fixtureWithChange(EnvironmentChangeDef breakfastChange) {
        EnvironmentResourceDef resource = new EnvironmentResourceDef(ENVIRONMENT_ID, DIMENSION_ID, List.of(
                state("night", List.of(new ClearBlockChangeDef(PLATE, SourceLocation.unknown()))),
                state("morning_breakfast", List.of(breakfastChange)),
                state("after_breakfast", List.of(new ClearBlockChangeDef(PLATE, SourceLocation.unknown())))),
                SourceLocation.unknown());
        EnvironmentCatalogState catalog = new EnvironmentCatalogState(NOW);
        catalog.install(new EnvironmentResourceCandidate(true,
                Map.of(ENVIRONMENT_ID, new EnvironmentCatalogEntry(resource, "test")), List.of(), 1, 0), NOW);
        FakeEnvironmentBlockAccess world = new FakeEnvironmentBlockAccess(DIMENSION_ID);
        world.register("displaydelight:bacon_and_eggs", Map.of("facing", Set.of("north", "south")));
        return new Fixture(new EnvironmentService(catalog), world);
    }

    private static EnvironmentStateDef state(String id, List<EnvironmentChangeDef> changes) {
        return new EnvironmentStateDef(id, changes, SourceLocation.unknown());
    }

    private record Fixture(EnvironmentService service, FakeEnvironmentBlockAccess world) {
        Optional<EnvironmentBlockAccess> world(NamespacedId dimension) {
            return dimension.equals(world.dimensionId()) ? Optional.of(world) : Optional.empty();
        }
    }
}
