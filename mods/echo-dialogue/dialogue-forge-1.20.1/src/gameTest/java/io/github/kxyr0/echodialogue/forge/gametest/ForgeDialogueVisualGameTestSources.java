package io.github.kxyr0.echodialogue.forge.gametest;

final class ForgeDialogueVisualGameTestSources {
    private ForgeDialogueVisualGameTestSources() {
    }

    static String portraitSource() {
        return """
                namespace EchoDialogue.VisualTest;

                class PortraitDialogue : DialogueResource
                {
                    private NodeDef[] dialogueNodes = [ Greeting() ];

                    public override DialogueResourceDef Build()
                    {
                        return Dialogue("echo_dialogue_test:portrait");
                    }

                    private void Init()
                    {
                        Start("greeting");
                        Alias("father", "echo_dialogue_test:father", display: "Отец",
                                visual: Portrait("echo_dialogue_test:father_portrait"));
                    }

                    private NodeDef[] Nodes()
                    {
                        return dialogueNodes;
                    }

                    private NodeDef Greeting()
                    {
                        var node = new Node("greeting");
                        Line("father", "Где ты был?");
                        Line("father", "Я ждал у дома.");
                        return node;
                    }
                }
                """;
    }

    static String skinHeadChoiceSource() {
        return skinHeadHeader() + skinHeadQuestionNode() + skinHeadAcceptedNode()
                + skinHeadRefusedNode() + sourceFooter();
    }

    private static String skinHeadHeader() {
        return """
                namespace EchoDialogue.VisualTest;

                class SkinHeadDialogue : DialogueResource
                {
                    private NodeDef[] dialogueNodes = [ Question(), Accepted(), Refused() ];

                    public override DialogueResourceDef Build()
                    {
                        return Dialogue("echo_dialogue_test:skin_head");
                    }

                    private void Init()
                    {
                        Start("question");
                        Alias("merchant", "echo_dialogue_test:merchant", display: "Торговец",
                                visual: SkinHead("echo_dialogue_test:merchant_skin"));
                        Alias("player", "echo_dialogue_test:player", display: "Игрок");
                    }

                    private NodeDef[] Nodes()
                    {
                        return dialogueNodes;
                    }

                """;
    }

    private static String skinHeadQuestionNode() {
        return """
                    private NodeDef Question()
                    {
                        var node = new Node("question");
                        Line("merchant", "Поделишься порошком?");
                        Choice("accept", "Да, держи.", next: "accepted");
                        Choice("refuse", "Нет.", next: "refused");
                        return node;
                    }

                """;
    }

    private static String skinHeadAcceptedNode() {
        return """
                    private NodeDef Accepted()
                    {
                        var node = new Node("accepted");
                        Line("player", "Бери.");
                        Complete("echo_dialogue_test:accepted");
                        return node;
                    }

                """;
    }

    private static String skinHeadRefusedNode() {
        return """
                    private NodeDef Refused()
                    {
                        var node = new Node("refused");
                        Line("player", "Не сегодня.");
                        Complete("echo_dialogue_test:refused");
                        return node;
                    }
                """;
    }

    private static String sourceFooter() {
        return """
                }
                """;
    }
}
