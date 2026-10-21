namespace EchoStoryNpcTest;

class TeleportFatherStory : StoryResource
{
    private void Nodes()
    {
        Start("start", next: "teleport");
        Action(
            "teleport",
            type: "echo_npc:teleport",
            actorId: "echo_story_npc_test:john_father",
            worldId: "minecraft:overworld",
            x: "5",
            y: "64",
            z: "0",
            yaw: "90",
            pitch: "0",
            output: "result",
            next: "finish");
        Finish("finish");
    }

    public override StoryResourceDef Build()
    {
        return Story("echo_story_npc_test:teleport_father", start: "start");
    }
}
