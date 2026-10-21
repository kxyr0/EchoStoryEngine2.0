namespace JDH.Chapter1.Day2;

class ChemistryClass : SceneResource
{
    Scene Start()
    {
        Time.Set("09:55");
        Nolan.MoveTo(UniversityHall);
        Dialogue.Play("jdh:chemistry_class_intro");
    }
}
