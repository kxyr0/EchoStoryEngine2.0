package io.github.kxyr0.echoitem.core.reload;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.definition.ItemDefinitionRevision;
import io.github.kxyr0.echoitem.core.catalog.ItemCatalogSnapshot;
import io.github.kxyr0.echoitem.core.script.EchoScriptWeaponCompiler;
import io.github.kxyr0.echoitem.core.source.ItemSourceCandidate;
import io.github.kxyr0.echoitem.core.source.ItemSourceOrigin;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemReloadPlannerTest {
    @Test
    void packagedOnlyPublishesDefinition() {
        ItemReloadPlanner planner = planner();

        var outcome = planner.reload(ItemCatalogSnapshot.empty(), List.of(source(
                "packaged/items/void_sword.weapon.es", ItemSourceOrigin.PACKAGED, "Void Sword", 10)));

        assertTrue(outcome.applied());
        assertEquals(10.0D, outcome.snapshot().weapon(EchoItemId.parse("jdh:void_sword"))
                .orElseThrow().combat().attackDamage());
    }

    @Test
    void validExternalOverrideWinsOverPackagedDefinition() {
        ItemReloadPlanner planner = planner();
        var packaged = source("packaged/items/void_sword.weapon.es", ItemSourceOrigin.PACKAGED, "Packaged", 10);
        var external = source("story/item-pack/jdh/items/weapons/swords/void_sword.weapon.es",
                ItemSourceOrigin.EXTERNAL, "External", 15);

        var outcome = planner.reload(ItemCatalogSnapshot.empty(), List.of(packaged, external));

        assertTrue(outcome.applied());
        assertEquals("External", outcome.snapshot().weapon(EchoItemId.parse("jdh:void_sword"))
                .orElseThrow().text().name());
    }

    @Test
    void oneChangedWeaponCompilesOnceAndUnchangedDoesNotReparse() {
        ItemReloadPlanner planner = planner();
        ItemSourceCandidate first = source("damage: 10");
        var initial = planner.reload(ItemCatalogSnapshot.empty(), List.of(first));
        planner.remember(List.of(first));

        ItemSourceCandidate changed = source("damage: 15");
        var reload = planner.reload(initial.snapshot(), List.of(changed));
        planner.remember(List.of(changed));
        var unchanged = planner.reload(reload.snapshot(), List.of(changed));

        assertTrue(reload.applied());
        assertEquals(1, reload.counters().changedWeaponFiles());
        assertEquals(1, reload.counters().parserCalls());
        assertEquals(1, reload.counters().binderCalls());
        assertEquals(0, reload.counters().unchangedParserCalls());
        assertEquals(0, unchanged.counters().parserCalls());
    }

    @Test
    void malformedCandidateRetainsPreviousKnownGood() {
        ItemReloadPlanner planner = planner();
        var initial = planner.reload(ItemCatalogSnapshot.empty(), List.of(source("damage: 10")));

        var broken = planner.reload(initial.snapshot(), List.of(source("damage: -1")));

        assertFalse(broken.applied());
        assertEquals(initial.snapshot().revision(), broken.snapshot().revision());
        assertTrue(broken.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code()
                .equals("LAST_KNOWN_GOOD_RETAINED")));
    }

    @Test
    void brokenExternalOverrideDoesNotRevealPackagedFallbackOverKnownGood() {
        ItemReloadPlanner planner = planner();
        var packaged = source("packaged/items/void_sword.weapon.es", ItemSourceOrigin.PACKAGED, "Packaged", 10);
        var external = source("story/item-pack/jdh/items/weapons/swords/void_sword.weapon.es",
                ItemSourceOrigin.EXTERNAL, "External", 15);
        var initial = planner.reload(ItemCatalogSnapshot.empty(), List.of(packaged, external));

        var broken = planner.reload(initial.snapshot(), List.of(packaged, brokenExternal()));

        assertFalse(broken.applied());
        assertEquals("External", broken.snapshot().weapon(EchoItemId.parse("jdh:void_sword"))
                .orElseThrow().text().name());
    }

    @Test
    void externalOverrideDeletionRevealsPackagedFallback() {
        ItemReloadPlanner planner = planner();
        var packaged = source("packaged/items/void_sword.weapon.es", ItemSourceOrigin.PACKAGED, "Packaged", 10);
        var external = source("story/item-pack/jdh/items/weapons/swords/void_sword.weapon.es",
                ItemSourceOrigin.EXTERNAL, "External", 15);
        var initial = planner.reload(ItemCatalogSnapshot.empty(), List.of(packaged, external));

        var deleted = planner.reload(initial.snapshot(), List.of(packaged));

        assertTrue(deleted.applied());
        assertEquals("Packaged", deleted.snapshot().weapon(EchoItemId.parse("jdh:void_sword"))
                .orElseThrow().text().name());
    }

    @Test
    void duplicateItemIdInSameOriginRejectsBatchAtomically() {
        ItemReloadPlanner planner = planner();
        var first = source("external/a/void_sword.weapon.es", ItemSourceOrigin.EXTERNAL, "First", 10);
        var second = source("external/b/void_sword.weapon.es", ItemSourceOrigin.EXTERNAL, "Second", 12);

        var duplicate = planner.reload(ItemCatalogSnapshot.empty(), List.of(first, second));

        assertFalse(duplicate.applied());
        assertTrue(duplicate.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code()
                .equals("DUPLICATE_ITEM_ID")));
    }

    @Test
    void deterministicRevisionAdvancesOncePerAppliedBatch() {
        ItemReloadPlanner planner = planner();
        var first = planner.reload(ItemCatalogSnapshot.empty(), List.of(source("damage: 10")));
        var second = planner.reload(first.snapshot(), List.of(source("damage: 12")));

        assertEquals(1L, first.snapshot().revision().value());
        assertEquals(2L, second.snapshot().revision().value());
    }

    private ItemReloadPlanner planner() {
        return new ItemReloadPlanner(new ItemCandidateCompiler(new EchoScriptWeaponCompiler()));
    }

    private ItemSourceCandidate source(String damageLine) {
        return source("void_sword.weapon.es", ItemSourceOrigin.EXTERNAL, "Void Sword", damageLine);
    }

    private ItemSourceCandidate source(String sourceId, ItemSourceOrigin origin, String name, int damage) {
        return source(sourceId, origin, name, "damage: " + damage);
    }

    private ItemSourceCandidate brokenExternal() {
        return source("story/item-pack/jdh/items/weapons/swords/void_sword.weapon.es",
                ItemSourceOrigin.EXTERNAL, "Broken", "damage: 0");
    }

    private ItemSourceCandidate source(String sourceId, ItemSourceOrigin origin, String name, String damageLine) {
        String text = """
                class VoidSword : WeaponResource
                {
                    public override WeaponDefinition Build()
                    {
                        return Weapon(
                            id: "jdh:void_sword",
                            name: "%s",
                            texture: "jdh:item/void_sword",
                            %s,
                            attacksPerSecond: "1.6",
                            durability: 200
                        );
                    }
                }
                """.formatted(name, damageLine);
        return new ItemSourceCandidate(sourceId, text, origin);
    }
}
