namespace JDH.Animation;

class SleepAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation("jdh:sasha/sleep", rig: "jdh:sasha",
            surfaces: [World]);
    }
}

class World : FacialAnimationSurface3d
{
    private PhaseDef Enter()
    {
        return Phase(durationTicks: 5, tracks: [
            Track("upper_eyelid_left", frames: [
                Frame(0).Position(0, 0, 0),
                Frame(5).Position(0, "0.07", 0)
            ])
        ]);
    }

    private PhaseDef Stay()
    {
        return Phase(durationTicks: 5, tracks: [
            Track("upper_eyelid_left", frames: [
                Frame(0).Position(0, "0.07", 0),
                Frame(5).Position(0, "0.07", 0)
            ])
        ]);
    }

    private PhaseDef Exit()
    {
        return Phase(durationTicks: 5, tracks: [
            Track("upper_eyelid_left", frames: [
                Frame(0).Position(0, "0.07", 0),
                Frame(5).Position(0, 0, 0)
            ])
        ]);
    }
}
