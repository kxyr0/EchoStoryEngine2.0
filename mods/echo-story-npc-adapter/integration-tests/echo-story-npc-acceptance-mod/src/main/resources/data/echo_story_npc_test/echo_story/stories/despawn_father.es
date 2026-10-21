namespace EchoStoryNpcTest;

class DespawnFatherStory : StoryResource
{
    private void Nodes()
    {
        Start("start", next: "despawn");
        Action(
            "despawn",
            type: "echo_npc:despawn",
            actorId: "echo_story_npc_test:john_father",
            output: "result",
            next: "finish");
        Finish("finish");
    }

    public override StoryResourceDef Build()
    {
        return Story("echo_story_npc_test:despawn_father", start: "start");
    }
}
