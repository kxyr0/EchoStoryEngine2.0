package io.github.kxyr0.echodialogue.echoscript;

final class EchoScriptDialogueSources {
    private EchoScriptDialogueSources() {
    }

    static String validSource() {
        return """
                namespace EchoDialogue.Sample;

                class SampleGreeting : DialogueResource
                {
                    private NodeDef[] dialogueNodes = [ Greeting() ];

                    public override DialogueResourceDef Build()
                    {
                        return Dialogue("sample:greeting");
                    }

                    private void Init()
                    {
                        Start("greeting");
                        Alias("father", "sample:father", display: "Отец");
                        Alias("player", "sample:player", display: "Игрок");
                    }

                    private NodeDef[] Nodes()
                    {
                        return dialogueNodes;
                    }

                    private NodeDef Greeting()
                    {
                        var node = new Node("greeting");

                        Line("father", "Где ты был?");
                        Line("player", "Гулял.");
                        Line("father", "Понятно.");

                        return node;
                    }
                }
                """;
    }

    static String duplicateNodeMethod() {
        return """
                    private NodeDef Duplicate()
                    {
                        var node = new Node("greeting");
                        Line("father", "Дубликат.");
                        return node;
                    }

                    private NodeDef Greeting()""";
    }

}

