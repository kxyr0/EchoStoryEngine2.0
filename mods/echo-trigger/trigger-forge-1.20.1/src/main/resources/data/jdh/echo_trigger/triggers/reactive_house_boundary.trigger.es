class ReactiveHouseBoundary : TriggerResource
{
    private TriggerArea House = BoxArea(Vector3(180, 64, 192), Vector3(188, 70, 200));

    private void Init()
    {
        Id("jdh:reactive_house_boundary");
        Scope(PLAYER);
        Dimension("minecraft:overworld");
        Bind(House);
        Cooldown(Ticks(40));
        StayInterval(Ticks(20));
        StayPolicy(SKIP_WHILE_RUNNING);
    }

    private void OnTriggerEnter(PlayerRef player)
    {
        Dialogue.Start("sample:greeting");
    }

    private void OnTriggerExit(TriggerEvent event)
    {
        Debug.Mark("house_exit");
    }
}
