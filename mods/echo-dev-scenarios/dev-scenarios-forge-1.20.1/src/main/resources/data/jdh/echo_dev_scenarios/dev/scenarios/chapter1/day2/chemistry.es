namespace JDH.Dev.Chapter1.Day2;

class ChemistryClass : DevScenarioResource
{
    Given()
    {
        Time.Set(Day(2), "09:55");
        Player.Place(UniversityHall);
        Npc.Ensure(Nolan);
        Npc.Ensure(Chris);
        Npc.Ensure(Halden);
    }

    Launch("jdh:chemistry_class");
}
