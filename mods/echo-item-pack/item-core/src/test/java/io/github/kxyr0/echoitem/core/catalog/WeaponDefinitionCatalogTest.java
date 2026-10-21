package io.github.kxyr0.echoitem.core.catalog;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.definition.ItemDefinitionRevision;
import io.github.kxyr0.echoitem.core.reload.ItemCandidateCompiler;
import io.github.kxyr0.echoitem.core.reload.ItemReloadPlanner;
import io.github.kxyr0.echoitem.core.script.EchoScriptWeaponCompiler;
import io.github.kxyr0.echoitem.core.source.ItemSourceCandidate;
import io.github.kxyr0.echoitem.core.source.ItemSourceOrigin;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WeaponDefinitionCatalogTest {
    @Test
    void sameCatalogBackedItemIdResolvesNewDefinitionAfterPublish() {
        WeaponDefinitionCatalog catalog = new WeaponDefinitionCatalog();
        ItemReloadPlanner planner = new ItemReloadPlanner(new ItemCandidateCompiler(new EchoScriptWeaponCompiler()));
        EchoItemId id = EchoItemId.parse("jdh:void_sword");

        var first = planner.reload(ItemCatalogSnapshot.empty(), List.of(source("Void Sword", 10)));
        catalog.publish(first.snapshot());
        var second = planner.reload(first.snapshot(), List.of(source("Void Sword+", 15)));
        catalog.publish(second.snapshot());

        assertEquals("Void Sword+", catalog.current(id).text().name());
        assertEquals(15.0D, catalog.current(id).combat().attackDamage());
    }

    @Test
    void missingDefinitionUsesUnavailableFallback() {
        var missing = WeaponDefinitionCatalog.unavailable(EchoItemId.parse("jdh:missing"),
                new ItemDefinitionRevision(2L));

        assertFalse(missing.available());
        assertEquals(1.0D, missing.combat().attackDamage());
    }

    private ItemSourceCandidate source(String name, int damage) {
        String text = """
                class VoidSword : WeaponResource
                {
                    public override WeaponDefinition Build()
                    {
                        return Weapon(
                            id: "jdh:void_sword",
                            name: "%s",
                            texture: "jdh:item/void_sword",
                            damage: %d
                        );
                    }
                }
                """.formatted(name, damage);
        return new ItemSourceCandidate("void_sword.weapon.es", text, ItemSourceOrigin.EXTERNAL);
    }
}
