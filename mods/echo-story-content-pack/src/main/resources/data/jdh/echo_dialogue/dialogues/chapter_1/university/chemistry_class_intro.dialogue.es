namespace JDH.Chapter1.University;

class ChemistryClassIntroDialogue : DialogueResource
{
    private NodeDef[] dialogueNodes = [ Intro() ];

    public override DialogueResourceDef Build()
    {
        return Dialogue("jdh:chemistry_class_intro");
    }

    private void Init()
    {
        Id("jdh:chemistry_class_intro");
        Start("intro");
        Alias("nolan", "jdh:nolan", display: "Нолан", visual: Portrait("jdh:nolan"));
    }

    private NodeDef[] Nodes()
    {
        return dialogueNodes;
    }

    private NodeDef Intro()
    {
        var node = new Node("intro");
        Line("nolan", "Начинаем занятие. Откройте конспекты и приготовьтесь к опыту.");
        Complete("jdh:chemistry_class_started");
        return node;
    }
}
