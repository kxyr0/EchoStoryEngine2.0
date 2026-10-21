namespace JDH.Chapter1.Home;

class StartStoryDialogue : DialogueResource
{
    private NodeDef[] dialogueNodes = [ StartStory() ];
    private CutsceneId Storytelling = "jdh:story_telling_sister_005";

    public override DialogueResourceDef Build()
    {
        return Dialogue("jdh:start_story");
    }

    private void Init()
    {
        Id("jdh:start_story");
        Start("start_story");
        Alias("sasha", "jdh:sasha", display: "Саша", visual: Portrait("jdh:sasha"));
        Alias(
            "john",
            "jdh:john",
            display: "Лололошка",
            nameColor: nameColor("#FC9004"),
            visual: SkinHead("echo_content_player:player/base"));
    }

    private NodeDef[] Nodes()
    {
        return dialogueNodes;
    }

    private NodeDef StartStory()
    {
        var node = new Node("start_story");
        ClearFaceAnimations();
        ClearBlinkTiming(sasha);
        ClearBlinkTiming(john);
        Cutscene.Start(Storytelling, holdLastFrame: true);
        Cutscene.WaitPrepared(Storytelling);
        Screen.SetBlack(0, 300);
        Line("john", "В долине, скрытой за гребнем Вечных Гор, лежало королевство Мнемозия.");
        Line(
            "john",
            "Это было место удивительной красоты: башни из белого камня тянулись к небу, а мосты через бурные реки были сплетены из серебристой лозы");
        Line("john", "Но над Мнемозией всегда висел густой, молочно-белый туман.");
        Line("sasha", "Ну я ёпта в шоке, я ебала");
        Cutscene.Stop(Storytelling);
        Complete("jdh:start_story_opened");
        return node;
    }
}
