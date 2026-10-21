class MollyBackpackTrigger : TriggerResource
{
    private TriggerArea Area = CircleArea(Vector3(37, -3, 100), 2);

    private void Init()
    {
        Id("jdh:molly_backpack_trigger");
        Dimension("minecraft:overworld");
        Bind(Area);
    }

    private void OnTriggerEnter(PlayerRef player)
    {
        Npc.EquipBackpack("jdh:molly", Position(37, -3, 100));
    }
}
