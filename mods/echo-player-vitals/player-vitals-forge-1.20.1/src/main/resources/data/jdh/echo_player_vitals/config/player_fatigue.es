namespace JDH.Configs.PlayerVitals;

class PlayerFatigueConfig : PlayerVitalsConfigResource
{
    private void Build()
    {
        PlayerFatigue(
            id: "jdh:player_fatigue",
            maximum: 100,
            restedReserveAbove: 0.50,
            tiredReserveAtMost: 0.50,
            tiredReserveAbove: 0.25,
            exhaustedReserveAtMost: 0.25,
            tiredMovementMultiplier: 0.95,
            exhaustedMovementMultiplier: 0.85,
            exhaustedNausea: 0.35
        );
    }
}
