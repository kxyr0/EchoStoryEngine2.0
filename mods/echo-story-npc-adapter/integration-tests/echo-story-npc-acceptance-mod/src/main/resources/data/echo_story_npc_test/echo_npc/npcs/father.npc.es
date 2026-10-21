namespace EchoStoryNpcTest;

class FatherNpc : NpcResource
{
    public override NpcResourceDef Build()
    {
        return Npc(
            id: "echo_story_npc_test:father",
            display: "Отец",
            skin: Skin("echo_story_npc_test:father")
        );
    }
}
