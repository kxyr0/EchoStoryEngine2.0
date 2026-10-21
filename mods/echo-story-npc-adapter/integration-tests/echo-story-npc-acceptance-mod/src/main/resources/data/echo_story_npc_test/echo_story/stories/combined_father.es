namespace EchoStoryNpcTest;

class CombinedFatherStory : StoryResource
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
            next: "teleport");
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
            next: "despawn");
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
        return Story("echo_story_npc_test:combined_father", start: "start");
    }
}
