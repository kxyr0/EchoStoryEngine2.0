namespace jdh.triggers.chapter_1;

class LeaveHouse : TriggerResource
{
    private Area Hall =
        Area("minecraft:overworld", 180, 4, 192, 4, 3, 4);

    private Area Stairs =
        Area("minecraft:overworld", 190, 4, 196, 3, 4, 3);

    private Area Exit =
        Area("minecraft:overworld", 200, 4, 200, 3, 3, 3);

    private void Init()
    {
        Id("jdh:leave_house");
        Scope(PLAYER);
        Mode(ONCE);
        Start(LeaveHouseSequence);
    }

    private void LeaveHouseSequence()
    {
        Wait(PlayerEnter(Hall));
        Wait(PlayerEnter(Stairs));
        Wait(PlayerEnter(Exit));

        Complete();
    }
}
