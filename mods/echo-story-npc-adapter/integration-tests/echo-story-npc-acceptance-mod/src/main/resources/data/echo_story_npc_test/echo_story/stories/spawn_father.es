namespace EchoStoryNpcTest;

class SpawnFatherStory : StoryResource
{
    private void Nodes()
    {
        Start("start", next: "spawn");
        Action(
            "spawn",
            type: "echo_npc:spawn",
            actorId: "echo_story_npc_test:john_father",
            definitionId: "echo_story_npc_test:father",
            worldId: "minecraft:overworld",
            x: "0",
            y: "64",
            z: "0",
            yaw: "0",
            pitch: "0",
            output: "result",
            next: "finish");
        Finish("finish");
    }

    public override StoryResourceDef Build()
    {
        return Story("echo_story_npc_test:spawn_father", start: "start");
    }
}
