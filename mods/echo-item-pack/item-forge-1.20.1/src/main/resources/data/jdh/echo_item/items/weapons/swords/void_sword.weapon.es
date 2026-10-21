namespace EchoItemPack.Jdh.Items.Weapons;

class VoidSword : WeaponResource
{
    private string ItemId = "jdh:void_sword";
    private string DisplayName = "Void Sword";
    private string DescriptionText = "A test blade loaded from EchoScript.";
    private string[] TooltipLines = ["Damage and speed can hot reload.", "Texture is resolved from the item-pack assets."];
    private string TextureRef = "jdh:item/void_sword";
    private string ModelRef = "handheld";
    private int DamageValue = 13;
    private string AttackSpeed = "1.6";
    private int MaxDurability = 2031;
    private int EnchantabilityValue = 18;
    private bool HasGlint = true;
    private bool FireProof = true;
    private int Order = 10;

    public override WeaponDefinition Build()
    {
        return Weapon(
            id: Id(),
            name: Name(),
            description: Description(),
            tooltip: Tooltip(),
            texture: Texture(),
            model: Model(),
            damage: Damage(),
            attacksPerSecond: AttacksPerSecond(),
            durability: Durability(),
            enchantability: Enchantability(),
            rarity: Rarity(),
            glint: Glint(),
            fireResistant: FireResistant(),
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

    private string[] Tooltip()
    {
        return TooltipLines;
    }

    private string Texture()
    {
        return TextureRef;
    }

    private string Model()
    {
        return ModelRef;
    }

    private int Damage()
    {
        return DamageValue;
    }

    private string AttacksPerSecond()
    {
        return AttackSpeed;
    }

    private int Durability()
    {
        return MaxDurability;
    }

    private int Enchantability()
    {
        return EnchantabilityValue;
    }

    private WeaponRarity Rarity()
    {
        return EPIC;
    }

    private bool Glint()
    {
        return HasGlint;
    }

    private bool FireResistant()
    {
        return FireProof;
    }

    private int CreativeOrder()
    {
        return Order;
    }
}
