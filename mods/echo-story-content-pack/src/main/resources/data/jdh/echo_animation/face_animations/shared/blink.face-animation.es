namespace JDH.Animation.Shared;

class SharedBlinkAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation("jdh:shared/blink", surfaces: [World, Portrait]);
    }
}

class World : FacialAnimationSurface3d
{
    private PhaseDef Enter()
    {
        return Phase(durationTicks: 3, tracks: [
            Track("upper_eyelids", frames: [Frame(0).Position(0, 0, 0), Frame(3).Position(0, "0.035", 0)]),
            Track("lower_eyelids", frames: [Frame(0).Position(0, 0, 0), Frame(3).Position(0, "-0.035", 0)])
        ]);
    }

    private PhaseDef Stay()
    {
        return Phase(durationTicks: 2, end: "HOLD", tracks: [
            Track("upper_eyelids", frames: [Frame(0).Position(0, "0.035", 0), Frame(2).Position(0, "0.035", 0)]),
            Track("lower_eyelids", frames: [Frame(0).Position(0, "-0.035", 0), Frame(2).Position(0, "-0.035", 0)])
        ]);
    }

    private PhaseDef Exit()
    {
        return Phase(durationTicks: 4, tracks: [
            Track("upper_eyelids", frames: [Frame(0).Position(0, "0.035", 0), Frame(4).Position(0, 0, 0)]),
            Track("lower_eyelids", frames: [Frame(0).Position(0, "-0.035", 0), Frame(4).Position(0, 0, 0)])
        ]);
    }
}

class Portrait : FacialAnimationSurfacePortrait
{
    private PhaseDef Enter()
    {
        return Phase(durationTicks: 3, tracks: [
            Track("upper_eyelids", frames: [Frame(0).Position(0, 0, 0), Frame(3).Position(0, "0.018", 0)]),
            Track("lower_eyelids", frames: [Frame(0).Position(0, 0, 0), Frame(3).Position(0, "-0.016", 0)])
        ]);
    }

    private PhaseDef Stay()
    {
        return Phase(durationTicks: 2, end: "HOLD", tracks: [
            Track("upper_eyelids", frames: [Frame(0).Position(0, "0.018", 0), Frame(2).Position(0, "0.018", 0)]),
            Track("lower_eyelids", frames: [Frame(0).Position(0, "-0.016", 0), Frame(2).Position(0, "-0.016", 0)])
        ]);
    }

    private PhaseDef Exit()
    {
        return Phase(durationTicks: 4, tracks: [
            Track("upper_eyelids", frames: [Frame(0).Position(0, "0.018", 0), Frame(4).Position(0, 0, 0)]),
            Track("lower_eyelids", frames: [Frame(0).Position(0, "-0.016", 0), Frame(4).Position(0, 0, 0)])
        ]);
    }
}
