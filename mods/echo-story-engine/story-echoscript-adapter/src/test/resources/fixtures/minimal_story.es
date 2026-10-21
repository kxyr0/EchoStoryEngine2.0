namespace EchoStoryEngine.Story.Resources;

class MinimalStory : StoryResource
{
    public override StoryResourceDef Build()
    {
        return Story("sample:hello_story", start: "start");
    }

    private void Nodes()
    {
        Start("start", next: "set_ready");
        Set("set_ready", variable: "sample.ready", value: true, next: "require_ready");
        Require("require_ready", variable: "sample.ready", value: true, next: "confirm");
        Action("confirm", type: "sample:confirm", next: "finish");
        Finish("finish");
    }
}
