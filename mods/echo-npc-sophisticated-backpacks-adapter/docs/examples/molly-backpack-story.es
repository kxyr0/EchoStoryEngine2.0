namespace JDH.Chapter1.Home;

class MollyGetsReady : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:molly_gets_ready", start: "start");
    }

    private void OnStart()
    {
        Npc.EquipBackpack(
            "jdh:molly",
            Position(37, -3, 100)
        );

        Npc.UnequipBackpack(
            "jdh:molly",
            Position(39, -3, 100)
        );
    }
}
