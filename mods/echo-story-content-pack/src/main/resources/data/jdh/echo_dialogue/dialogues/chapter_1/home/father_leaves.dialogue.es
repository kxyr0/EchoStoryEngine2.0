namespace JDH.Chapter1.Home;

class FatherLeavesDialogue : DialogueResource
{
    private NodeDef[] dialogueNodes = [ Leaving() ];

    public override DialogueResourceDef Build()
    {
        return Dialogue("jdh:father_leaves");
    }

    private void Init()
    {
        Id("jdh:father_leaves");
        Start("leaving");
        Alias("father", "jdh:john_father", display: "Отец", visual: Portrait("jdh:john_father"));
        Alias("john", "jdh:john", display: "Лололошка", visual: SkinHead("echo_content_player:player/base"));
    }

    private NodeDef[] Nodes()
    {
        return dialogueNodes;
    }

    private NodeDef Leaving()
    {
        var node = new Node("leaving");
        Line("father", "Я проверю дорогу у ворот.");
        Npc.Move(father, 40, -4, 114);
        Line("father", "Дойду и осмотрю всё по пути.");
        LineAfterMove("father", "Я на месте. Подойди, когда будешь готов.");
        Line("john", "Хорошо, я скоро буду.");
        Complete("jdh:father_reached_gate");
        return node;
    }
}
