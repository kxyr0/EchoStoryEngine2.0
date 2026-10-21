namespace JDH.Chapter1.Home;

class Chapter1OpeningStory : StoryResource
{
    Id("jdh:chapter_1_opening");

    OnStart()
    {
        Story.Set("chapter_1.opening_started", true);
        Screen.SetBlack(0, 1);

        Npc.SpawnAt("jdh:sasha", 48, -2, 92, yaw: 180, pitch: 0);
        Player.SpawnAt(46.5868, -2.0, 92.0620, yaw: -16, pitch: 24);
        Npc.SleepAt("jdh:sasha", 49, -3, 92);

        Player.SetHotbarItem(1, "minecraft:book", count: 1);
        Player.SelectHotbarSlot(1);
        Player.SitAt(48, -3, 90, timeoutTicks: 40);

        Dialogue.Load("jdh:start_story", audienceId: "jdh:local_author", output: "outcome");

        Story.Set("chapter_1.opening_completed", true);
    }
}
