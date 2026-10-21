namespace EchoStoryEngine.Story.Dialogues.Chapter1;

class Chapter1Home : DialogueResource
{
    private StateRef homeDone = State.Memory("chapter_1.home_done");

    private NodeDef[] dialogueNodes = [ Home() ];

    public override StoryResourceDef Build()
    {
        return Dialogue();
    }

    private void Init()
    {
        Start("home");
        Alias("j", "jdh");
        Alias("jf", "john_father");
    }

    private NodeDef[] Nodes()
    {
        return dialogueNodes;
    }

    private Effect[] CompleteEffects()
    {
        Set(homeDone, true);
    }

    private NodeDef Home()
    {
        var node = new Node("home");

        Line("jf", "Стоять", key: "chapter_1.home.line_1", face: "state_angry_hold");
        Line("jf", "Ну и где ты был?", key: "chapter_1.home.line_2");
        Line("j", "Гулял.", key: "chapter_1.home.line_3");
        Line("jf", "Может раскажешь по хорошему?", key: "chapter_1.home.line_4");
        Line("j", "Ага, я всё ещё помню что ты сделал.", key: "chapter_1.home.line_5");
        Line("jf", "Да ты что, тебе мало?", key: "chapter_1.home.line_6");
        Line("jf", "Вали в комнату.", key: "chapter_1.home.line_7");
        Line("jf", "Позже поговорим.", key: "chapter_1.home.line_8");

        return node;
    }
}
