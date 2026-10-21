namespace JDH.Chapter1.Home;

class MollyBackpackDialogue : DialogueResource
{
    private BlockPosition BackpackPlace = new BlockPosition(39, -3, 100);

    private void Init()
    {
        Id("jdh:molly_backpack_dialogue");
        Start(Backpack);
        Alias("Molly", "jdh:molly");
    }

    private void Backpack()
    {
        Npc.EquipBackpack(Molly, Position(37, -3, 100));
        Line("Molly", "Рюкзак взят.");
        Npc.UnequipBackpack(Molly, BackpackPlace);
        Line("Molly", "Рюкзак поставлен.");
    }
}
