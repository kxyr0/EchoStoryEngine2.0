package io.github.kxyr0.echodialogue.forge.gametest;

final class ForgeDialogueGameTestSources {
    private ForgeDialogueGameTestSources() {
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

    static String choiceSource() {
        return choiceSourceHeader() + choiceQuestionNode() + choiceAcceptedNode()
                + choiceRefusedNode() + choiceSourceFooter();
    }

    private static String choiceSourceHeader() {
        return """
                namespace EchoDialogue.Sample;

                class QuestionDialogue : DialogueResource
                {
                    private NodeDef[] dialogueNodes = [ Question(), Accepted(), Refused() ];

                    public override DialogueResourceDef Build()
                    {
                        return Dialogue("sample:question");
                    }

                    private void Init()
                    {
                        Start("question");
                        Alias("father", "sample:father", display: "Father");
                        Alias("player", "sample:player", display: "Player");
                    }

                    private NodeDef[] Nodes()
                    {
                        return dialogueNodes;
                    }

                """;
    }

    private static String choiceQuestionNode() {
        return """
                    private NodeDef Question()
                    {
                        var node = new Node("question");
                        Line("father", "Will you help me?");
                        Choice("accept", "Yes.", next: "accepted");
                        Choice("refuse", "No.", next: "refused");
                        return node;
                    }

                """;
    }

    private static String choiceAcceptedNode() {
        return """
                    private NodeDef Accepted()
                    {
                        var node = new Node("accepted");
                        Line("player", "Good.");
                        Complete("sample:accepted");
                        return node;
                    }

                """;
    }

    private static String choiceRefusedNode() {
        return """
                    private NodeDef Refused()
                    {
                        var node = new Node("refused");
                        Line("player", "Busy.");
                        Complete("sample:refused");
                        return node;
                    }
                """;
    }

    private static String choiceSourceFooter() {
        return """
                }
                """;
    }

}
