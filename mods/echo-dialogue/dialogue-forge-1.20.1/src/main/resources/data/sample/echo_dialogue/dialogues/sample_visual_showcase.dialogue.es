namespace EchoDialogue.Sample;

class SampleVisualShowcase : DialogueResource
{
    private void Init()
    {
        Id("sample:visual_showcase");
        Start(Greeting);
        Alias("father", "sample:father", display: "Отец", visual: SkinHead("sample:father"));
        Alias("merchant", "sample:merchant", display: "Торговец", visual: Portrait("sample:merchant"));
        Alias("player", "sample:player", display: "Игрок");
    }

    private void Greeting()
    {
        Line("father", "Ты опять пропал у реки.");
        Line("merchant", "У меня есть работа, если не боишься ночной дороги.");
        Choice("accept", "Взять поручение.", Accepted);
        Choice("refuse", "Отказаться.", Refused);
    }

    private void Accepted()
    {
        Line("player", "Хорошо, я помогу.");
        Complete("sample:visual_accept");
    }

    private void Refused()
    {
        Line("player", "Сейчас не могу.");
        Complete("sample:visual_refuse");
    }
}
