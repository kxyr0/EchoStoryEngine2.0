namespace JDH.Acceptance;

class ContentPackSmokeRefusedStory : StoryResource
{
    Id("jdh:content_pack_smoke_refused");

    OnStart()
    {
        Action(
            type: "echo_npc:spawn",
            actorId: "jdh:content_pack_father",
            definitionId: "jdh:content_pack_father",
            worldId: "minecraft:overworld",
            x: "0",
            y: "64",
            z: "0",
            yaw: "180",
            pitch: "0",
            output: "result");
        Dialogue.Load(
            "jdh:content_pack_greeting",
            audienceId: "00000000-0000-0000-0000-0000000000c1",
            output: "outcome");
        Story.Require("outcome", "jdh:content_pack_refused");
        Story.Set("jdh.content_pack.refused_branch", true);
    }
}
