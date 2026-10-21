namespace JDH.Chapter1.Home;

class HomeArrivalStory : StoryResource
{
    Id("jdh:chapter_1_home_arrival");

    OnStart()
    {
        Action(
            type: "echo_npc:spawn",
            actorId: "jdh:john_father",
            definitionId: "jdh:john_father",
            worldId: "minecraft:overworld",
            x: "0",
            y: "64",
            z: "0",
            yaw: "180",
            pitch: "0",
            output: "result");
        Dialogue.Load(
            "jdh:chapter_1_home_arrival",
            audienceId: "jdh:local_author",
            output: "outcome");
        Story.Set("jdh.chapter_1.home_arrival_dialogue_seen", true);
    }
}
