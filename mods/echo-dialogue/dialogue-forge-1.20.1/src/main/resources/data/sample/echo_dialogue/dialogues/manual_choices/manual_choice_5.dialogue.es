namespace EchoDialogue.Sample.Manual;

class ManualChoiceFive : DialogueResource
{
    private void Init()
    {
        Id("sample:manual_choice_5");
        Start(Question);
        Alias("father", "sample:father", display: "Отец");
        Alias("player", "sample:player", display: "Игрок");
    }

    private void Question()
    {
        Line("father", "Выбери вариант проверки 5.");
        Choice("one", "Текст выбора", One);
        Choice("two", "Текст выбора", Two);
        Choice("three", "Текст выбора", Three);
        Choice("four", "Текст выбора", Four);
    }

    private void One()
    {
        Line("player", "Выбран вариант 1.");
        Complete("sample:choice_one");
    }

    private void Two()
    {
        Line("player", "Выбран вариант 2.");
        Complete("sample:choice_two");
    }

    private void Three()
    {
        Line("player", "Выбран вариант 3.");
        Complete("sample:choice_three");
    }

    private void Four()
    {
        Line("player", "Выбран вариант 4.");
        Complete("sample:choice_four");
    }
}
