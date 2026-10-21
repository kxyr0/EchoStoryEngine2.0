namespace JDH.Test.Seats;

class BreakfastTableSeats : SeatZoneResource
{
    private SeatZoneId BreakfastTable = "jdh:test/breakfast_table";
    private SeatSlotId SeatOne = "jdh:test/breakfast_table/seat_1";
    private SeatSlotId SeatTwo = "jdh:test/breakfast_table/seat_2";
    private SeatSlotId MothersSeat = "jdh:test/breakfast_table/seat_3";
    private SeatSlotId SeatFour = "jdh:test/breakfast_table/seat_4";

    private void Init()
    {
        Id(BreakfastTable);
        Dimension("minecraft:overworld");
        Area(Box(Position(100, 64, 100), Position(104, 66, 104)));
        Detect(RIDING_ANCHOR);
        UnknownSeats(ALLOW);
        Slot(SeatOne, Position(101, 65, 101));
        Slot(SeatTwo, Position(103, 65, 101));
        Slot(MothersSeat, Position(103, 65, 103));
        Slot(SeatFour, Position(101, 65, 103));
        MatchRadius(0.85);
    }
}
