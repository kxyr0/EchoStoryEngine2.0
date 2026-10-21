namespace EchoDialogue.Sample;

class SampleGreeting : DialogueResource
{
    private void Init()
    {
        Id("sample:greeting");
        Start(Greeting);
        Alias("father", "sample:father", display: "Отец");
        Alias("player", "sample:player", display: "Игрок");
    }

    private void Greeting()
    {
        Line("father", "Где ты был?");
        Line("player", "Гулял.");
        Line("father", "Понятно.");
    }
}
