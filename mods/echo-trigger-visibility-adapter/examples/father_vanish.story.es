namespace JDH.Chapter1.Visibility;

class FatherVanishStory : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("jdh:father_vanish_story", start: "start");
    }

    private void OnStart()
    {
        View.Arm("jdh:father_vanish");
        View.WaitUntilOutOfView("jdh:father_vanish");
        View.AssertStillOutOfView("jdh:father_vanish");
        Environment.Set("jdh:father_state", "vanished");
        View.Disarm("jdh:father_vanish");
    }
}
