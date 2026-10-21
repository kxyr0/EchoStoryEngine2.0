namespace EchoDialogue.VisualTest;

class PortraitDialogue : DialogueResource
{
    private void Init()
    {
        Id("echo_dialogue_test:portrait");
        Start(Greeting);
        Alias("father", "echo_dialogue_test:father", display: "Отец",
                visual: Portrait("echo_dialogue_test:father_portrait"));
    }

    private void Greeting()
    {
        Line("father", "Где ты был?");
        Line("father", "Я ждал у дома.");
    }
}
