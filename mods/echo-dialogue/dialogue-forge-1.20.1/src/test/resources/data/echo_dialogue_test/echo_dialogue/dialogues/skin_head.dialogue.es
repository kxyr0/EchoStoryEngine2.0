namespace EchoDialogue.VisualTest;

class SkinHeadDialogue : DialogueResource
{
    private void Init()
    {
        Id("echo_dialogue_test:skin_head");
        Start(Question);
        Alias("merchant", "echo_dialogue_test:merchant", display: "Торговец",
                visual: SkinHead("echo_dialogue_test:merchant_skin"));
        Alias("player", "echo_dialogue_test:player", display: "Игрок");
    }

    private void Question()
    {
        Line("merchant", "Поделишься порошком?");
        Choice("accept", "Да, держи.", Accepted);
        Choice("refuse", "Нет.", Refused);
    }

    private void Accepted()
    {
        Line("player", "Бери.");
        Complete("echo_dialogue_test:accepted");
    }

    private void Refused()
    {
        Line("player", "Не сегодня.");
        Complete("echo_dialogue_test:refused");
    }
}
