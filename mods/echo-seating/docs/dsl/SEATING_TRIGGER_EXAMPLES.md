# Seating Trigger Examples

Generic breakfast trigger:

```csharp
class BreakfastSeatTrigger : TriggerResource
{
    private SeatZoneId BreakfastTable = "jdh:home/breakfast_table";
    private StoryId BreakfastStory = "jdh:chapter_1/breakfast";

    private void Init()
    {
        Id("jdh:breakfast_seat_trigger");
        Subscribe(Player.SeatedIn(BreakfastTable));
        StartStory(BreakfastStory);
        FireOnlyOncePerPlayer();
    }
}
```

Seat-specific trigger:

```csharp
class MothersSeatTrigger : TriggerResource
{
    private SeatZoneId BreakfastTable = "jdh:home/breakfast_table";
    private SeatSlotId MothersSeat = "jdh:home/breakfast_table/seat_3";

    private void Init()
    {
        Id("jdh:mothers_seat_trigger");
        Subscribe(Player.SeatedAt(BreakfastTable, MothersSeat));
        StartStory("jdh:chapter_1/mothers_seat");
        FireOnlyOncePerPlayer();
    }
}
```

Direct dialogue trigger:

```csharp
class MothersSeatDialogueTrigger : TriggerResource
{
    private SeatZoneId BreakfastTable = "jdh:home/breakfast_table";
    private SeatSlotId MothersSeat = "jdh:home/breakfast_table/seat_3";
    private DialogueId WrongSeatDialogue = "jdh:dialogue/wrong_mothers_seat";

    private void Init()
    {
        Id("jdh:mothers_seat_dialogue_trigger");
        Subscribe(Player.SeatedAt(BreakfastTable, MothersSeat));
        StartDialogue(WrongSeatDialogue);
    }
}
```
