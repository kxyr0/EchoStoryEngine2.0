namespace EchoDialogue.PresentationTest;

class DynamicProviderShowcase : DialogueResource
{
    private void Init()
    {
        Id("echo_dialogue_test:dynamic_provider_showcase");
        Start(Greeting);
        Alias("father", "echo_dialogue_test:dynamic_father", display: "Отец", visual: SkinHead("sample:father"));
        Alias("textOnly", "echo_dialogue_test:dynamic_text", display: "Рассказчик");
        Alias("unavailable", "echo_dialogue_test:unavailable", display: "Недоступный");
        Alias("failure", "echo_dialogue_test:failure", display: "Сбойный");
        Alias("malformed", "echo_dialogue_test:malformed", display: "Неверный");
        Alias("ambiguous", "echo_dialogue_test:ambiguous", display: "Спорный");
        Alias("plain", "echo_dialogue_test:not_handled", display: "Обычный");
    }

    private void Greeting()
    {
        Line("father", "Это строка должна сохранить исходный текст.");
        Line("textOnly", "Эта строка проверяет text-only dynamic presentation.");
        Line("plain", "Эта строка остаётся authored fallback.");
        Line("unavailable", "Эта строка проверяет unavailable fallback.");
        Line("failure", "Эта строка проверяет failure fallback.");
        Line("malformed", "Эта строка проверяет malformed fallback.");
        Line("ambiguous", "Эта строка проверяет ambiguity fallback.");
        Choice("done", "Завершить.", Done);
        Choice("again", "Повторить.", ChoiceNode);
    }

    private void ChoiceNode()
    {
        NodeId("choice");

        Line("father", "Выбор должен сохранить порядок.");
        Choice("one", "Первый выбор.", Done);
        Choice("two", "Второй выбор.", Done);
        Choice("three", "Третий выбор.", Done);
        Choice("four", "Четвёртый выбор.", Done);
    }

    private void Done()
    {
        Line("plain", "Готово.");
        Complete("echo_dialogue_test:done");
    }
}
