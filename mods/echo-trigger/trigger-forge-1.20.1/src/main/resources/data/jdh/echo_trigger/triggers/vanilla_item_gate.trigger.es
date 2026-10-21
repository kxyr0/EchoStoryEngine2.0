namespace jdh.triggers.chapter_1;

class VanillaItemGate : TriggerResource
{
    private void Init()
    {
        Id("jdh:vanilla_item_gate");
        Scope(PLAYER);
        Mode(ONCE);
        Start(Run);
    }

    private void Run()
    {
        Wait(HasItem("minecraft:diamond", count: 1));
        Wait(ItemAdded("#minecraft:wool", count: 2));
        Complete();
    }
}
