namespace EchoItemPack.Jdh.Items.Weapons;

class TrainingDagger : WeaponResource
{
    private string ItemId = "jdh:training_dagger";
    private string DisplayName = "Training Dagger";
    private string DescriptionText = "A fast practice weapon loaded from EchoScript.";
    private string[] TooltipLines = ["Lower damage, higher attack rate."];
    private string TextureRef = "jdh:item/training_dagger";
    private string ModelRef = "handheld";
    private int DamageValue = 4;
    private string AttackSpeed = "2.4";
    private int MaxDurability = 180;
    private int EnchantabilityValue = 5;
    private int Order = 20;

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
        return COMMON;
    }

    private int CreativeOrder()
    {
        return Order;
    }
}
