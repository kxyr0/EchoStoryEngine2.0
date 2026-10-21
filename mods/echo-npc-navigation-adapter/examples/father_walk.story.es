namespace JDH.Examples;

class FatherWalkStory : StoryResource
{
    private void Nodes()
    {
        Start("start", next: "move_father");
        Npc.Move("move_father", "jdh:father", 200, 64, 180, next: "finish");
        Finish("finish");
    }

    public override StoryResourceDef Build()
    {
        return Story("jdh:father_walk", start: "start");
    }
}
