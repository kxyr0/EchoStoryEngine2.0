class FatherGateTrigger : TriggerResource
{
    private TriggerArea Gate = BoxArea(Vector3(190, 60, 170), Vector3(210, 70, 190));

    private void Init()
    {
        Id("jdh:father_gate_trigger");
        Dimension("minecraft:overworld");
        Bind(Gate);
        FireOnlyOnce();
    }

    private void OnTriggerEnter(PlayerRef player)
    {
        Npc.Move("jdh:father", 200, 64, 180);
    }
}
