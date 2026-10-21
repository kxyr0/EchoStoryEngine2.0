namespace JDH.Acceptance;

class ContentPackGreetingDialogue : DialogueResource
{
    private void Init()
    {
        Id("jdh:content_pack_greeting");
        Start(Greeting);
        Alias("father", "jdh:content_pack_father", display: "Отец", visual: SkinHead("jdh:content_pack_father"));
        Alias("john", "jdh:content_pack_john", display: "Лололошка", visual: SkinHead("echo_content_player:player/base"));
    }

    private void Greeting()
    {
        Line("father", "Ты слышишь меня?");
        Line("father", "Ответь сейчас.");
        Choice("accept", "Да, я здесь.", Accepted);
        Choice("refuse", "Не сейчас.", Refused);
    }

    private void Accepted()
    {
        Line("john", "Да, я здесь.");
        Complete("jdh:content_pack_accepted");
    }

    private void Refused()
    {
        Line("john", "Не сейчас.");
        Complete("jdh:content_pack_refused");
    }
}
