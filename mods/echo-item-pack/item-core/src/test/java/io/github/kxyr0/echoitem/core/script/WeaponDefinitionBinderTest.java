package io.github.kxyr0.echoitem.core.script;

import io.github.kxyr0.echoitem.api.definition.ItemDefinitionRevision;
import io.github.kxyr0.echoitem.api.diagnostic.ItemDiagnosticCodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponDefinitionBinderTest {
    private final EchoScriptWeaponCompiler compiler = new EchoScriptWeaponCompiler();

    @Test
    void minimalValidWeaponUsesDefaults() {
        var result = compiler.compile("void_sword.weapon.es", minimal("jdh:void_sword"),
                new ItemDefinitionRevision(1L));

        assertTrue(result.successful());
        var weapon = result.definition().orElseThrow();
        assertEquals("jdh:void_sword", weapon.id().value());
        assertEquals(1.0D, weapon.combat().attackDamage());
        assertEquals(1.0D, weapon.combat().attacksPerSecond());
        assertEquals(0, weapon.durability().durability());
        assertTrue(weapon.durability().unbreakable());
    }

    @Test
    void fullValidWeaponBindsAllLiveFields() {
        var result = compiler.compile("void_sword.weapon.es", full("jdh:void_sword", 13, "1.6"),
                new ItemDefinitionRevision(7L));

        assertTrue(result.successful());
        var weapon = result.definition().orElseThrow();
        assertEquals("Void Sword", weapon.text().name());
        assertEquals(13.0D, weapon.combat().attackDamage());
        assertEquals(1.6D, weapon.combat().attacksPerSecond());
        assertEquals(2031, weapon.durability().durability());
        assertFalse(weapon.durability().unbreakable());
        assertEquals(7L, weapon.revision().value());
    }

    @Test
    void fieldLocalAndMethodValuesAreResolved() {
        var result = compiler.compile("void_sword.weapon.es", structured("jdh:void_sword"),
                new ItemDefinitionRevision(11L));

        assertTrue(result.successful(), () -> "diagnostics=" + result.diagnostics());
        var weapon = result.definition().orElseThrow();
        assertEquals("jdh:void_sword", weapon.id().value());
        assertEquals("Void Sword", weapon.text().name());
        assertEquals("jdh:item/void_sword", weapon.visual().texture());
        assertEquals(13.0D, weapon.combat().attackDamage());
        assertEquals(1.6D, weapon.combat().attacksPerSecond());
        assertTrue(weapon.durability().unbreakable());
        assertEquals(18, weapon.durability().enchantability());
        assertEquals("HEAD", weapon.equipmentSlot().orElseThrow().name());
    }

    @Test
    void missingRequiredFieldsAreDiagnostics() {
        var missingId = compiler.compile("void_sword.weapon.es", scriptWithout("id: \"jdh:void_sword\","),
                new ItemDefinitionRevision(1L));
        var missingName = compiler.compile("void_sword.weapon.es", scriptWithout("name: \"Void Sword\","),
                new ItemDefinitionRevision(1L));
        var missingTexture = compiler.compile("void_sword.weapon.es", scriptWithout("texture: \"jdh:item/void_sword\""),
                new ItemDefinitionRevision(1L));

        assertFalse(missingId.successful());
        assertFalse(missingName.successful());
        assertFalse(missingTexture.successful());
    }

    @Test
    void invalidWeaponValuesAreDiagnostics() {
        assertHasCode(full("jdh:void_sword", 0, "1.6"), ItemDiagnosticCodes.INVALID_ATTACK_DAMAGE);
        assertHasCode(full("jdh:void_sword", 13, "0"), ItemDiagnosticCodes.INVALID_ATTACK_SPEED);
        assertHasCode(full("jdh:void_sword", 13, "1.6").replace("durability: 2031", "durability: 0"),
                ItemDiagnosticCodes.INVALID_DURABILITY);
        assertHasCode(full("jdh:void_sword", 13, "1.6").replace("rarity: EPIC", "rarity: LEGENDARY"),
                ItemDiagnosticCodes.ITEM_SCRIPT_BIND_ERROR);
    }

    @Test
    void attributeConflictsAndUnknownAttributesAreRejected() {
        assertHasCode(full("jdh:void_sword", 13, "1.6").replace("damage: 13,", "damage: 13, attackDamage: 14,"),
                ItemDiagnosticCodes.ATTRIBUTE_CONFLICT);
        assertHasCode(full("jdh:void_sword", 13, "1.6").replace("damage: 13,", "damage: 13, unknown: 1,"),
                ItemDiagnosticCodes.INVALID_ATTRIBUTE);
        assertHasCode(full("jdh:void_sword", 13, "1.6").replace("attacksPerSecond: \"1.6\",",
                "attacksPerSecond: \"1.6\", attackSpeedModifier: \"2.4\","),
                ItemDiagnosticCodes.ATTRIBUTE_CONFLICT);
    }

    private void assertHasCode(String source, String code) {
        var result = compiler.compile("void_sword.weapon.es", source, new ItemDefinitionRevision(1L));
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals(code)),
                () -> "diagnostics=" + result.diagnostics());
        assertFalse(result.successful());
    }

    private String minimal(String id) {
        return """
                namespace EchoItemPack.Test;

                class VoidSword : WeaponResource
                {
                    public override WeaponDefinition Build()
                    {
                        return Weapon(
                            id: "%s",
                            name: "Void Sword",
                            texture: "jdh:item/void_sword"
                        );
                    }
                }
                """.formatted(id);
    }

    private String full(String id, int damage, String speed) {
        return minimal(id).replace("texture: \"jdh:item/void_sword\"",
                "texture: \"jdh:item/void_sword\",\n"
                        + "            damage: " + damage + ",\n"
                        + "            attacksPerSecond: \"" + speed + "\",\n"
                        + "            durability: 2031,\n"
                        + "            rarity: EPIC");
    }

    private String scriptWithout(String fragment) {
        return minimal("jdh:void_sword").replace(fragment, "");
    }

    private String structured(String id) {
        return """
                namespace EchoItemPack.Test;

                class VoidSword : WeaponResource
                {
                    private string ItemId = "%s";
                    private string DisplayName = "Void Sword";
                    private string DescriptionText = "A test blade loaded from EchoScript.";
                    private string TextureRef = "jdh:item/void_sword";
                    private string[] TooltipLines = ["Damage and speed can hot reload.", "Texture is resolved from the item-pack assets."];
                    private int DamageValue = 13;
                    private string AttackSpeed = "1.6";

                    public override WeaponDefinition Build()
                    {
                        var icon = Texture();

                        return Weapon(
                            id: Id(),
                            name: Name(),
                            description: Description(),
                            tooltip: Tooltip(),
                            texture: icon,
                            damage: Damage(),
                            attacksPerSecond: Speed(),
                            enchantability: Enchantability(),
                            rarity: Rarity(),
                            glint: Glint(),
                            fireResistant: FireResistant(),
                            unbreakable: Unbreakable(),
                            equipmentSlot: HEAD,
                            creativeOrder: CreativeOrder()
                        );
                    }

                    private string Id()
                    {
                        return ItemId;
                    }

                    private string Name()
                    {
                        return DisplayName;
                    }

                    private string Description()
                    {
                        return DescriptionText;
                    }

                    private string Texture()
                    {
                        return TextureRef;
                    }

                    private string[] Tooltip()
                    {
                        return TooltipLines;
                    }

                    private int Damage()
                    {
                        return DamageValue;
                    }

                    private string Speed()
                    {
                        return AttackSpeed;
                    }

                    private int Enchantability()
                    {
                        return 18;
                    }

                    private WeaponRarity Rarity()
                    {
                        return EPIC;
                    }

                    private bool Glint()
                    {
                        return true;
                    }

                    private bool FireResistant()
                    {
                        return true;
                    }

                    private bool Unbreakable()
                    {
                        return true;
                    }

                    private int CreativeOrder()
                    {
                        return 10;
                    }
                }
                """.formatted(id);
    }
}
