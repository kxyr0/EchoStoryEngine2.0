package io.github.kxyr0.echostoryengine.story.forge;

public final class ForgeStoryTestSources {
    private ForgeStoryTestSources() {
    }

    public static String minimalStory(String storyId, String actionType) {
        return """
                namespace EchoStoryEngine.Story.Resources;

                class MinimalStory : StoryResource
                {
                    public override StoryResourceDef Build()
                    {
                        return Story("%s", start: "start");
                    }

                    private void Nodes()
                    {
                        Start("start", next: "set_ready");
                        Set("set_ready", variable: "sample.ready", value: true, next: "require_ready");
                        Require("require_ready", variable: "sample.ready", value: true, next: "confirm");
                        Action("confirm", type: "%s", next: "finish");
                        Finish("finish");
                    }
                }
                """.formatted(storyId, actionType);
    }
}
