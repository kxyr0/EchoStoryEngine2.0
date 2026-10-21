namespace JDH.Config.Time;

class MainTimeSpeed : TimeSpeedConfigResource
{
    private void Init()
    {
        Id("jdh:main_time");
        InitialDay(1);
        InitialTime("08:30");
        Speed(gameMinutes: 1.0, realSeconds: 1.0);
        Running(true);
        SyncSun(true);
        SyncMoon(true);
        SunDimension("minecraft:overworld");
    }
}
