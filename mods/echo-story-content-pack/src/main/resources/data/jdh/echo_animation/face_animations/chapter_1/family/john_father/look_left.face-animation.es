namespace JDH.Animation.Chapter1.Family.JohnFather;

class JohnFatherLookLeftAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation("jdh:john_father/look_left", rig: "jdh:john_father", surfaces: [World, Portrait]);
    }
}

class World : FacialAnimationSurface3d
{
    private PhaseDef Enter()
    {
        return Phase(durationTicks: 6, tracks: [
            Track("left_pupil", frames: [Frame(0).Position(0, 0, 0), Frame(6).Position("-0.031", 0, 0)]),
            Track("right_pupil", frames: [Frame(0).Position(0, 0, 0), Frame(6).Position(0, 0, 0)])
        ]);
    }

    private PhaseDef Stay()
    {
        return Phase(durationTicks: 12, end: "HOLD", tracks: [
            Track("left_pupil", frames: [Frame(0).Position("-0.031", 0, 0), Frame(12).Position("-0.031", 0, 0)]),
            Track("right_pupil", frames: [Frame(0).Position(0, 0, 0), Frame(12).Position(0, 0, 0)])
        ]);
    }

    private PhaseDef Exit()
    {
        return Phase(durationTicks: 6, tracks: [
            Track("left_pupil", frames: [Frame(0).Position("-0.031", 0, 0), Frame(6).Position(0, 0, 0)]),
            Track("right_pupil", frames: [Frame(0).Position(0, 0, 0), Frame(6).Position(0, 0, 0)])
        ]);
    }
}

class Portrait : FacialAnimationSurfacePortrait
{
    private PhaseDef Enter()
    {
        return Phase(durationTicks: 6, tracks: [
            Track("left_pupil", frames: [Frame(0).Position(0, 0, 0), Frame(6).Position("-0.020", 0, 0)]),
            Track("right_pupil", frames: [Frame(0).Position(0, 0, 0), Frame(6).Position(0, 0, 0)])
        ]);
    }

    private PhaseDef Stay()
    {
        return Phase(durationTicks: 12, end: "HOLD", tracks: [
            Track("left_pupil", frames: [Frame(0).Position("-0.020", 0, 0), Frame(12).Position("-0.020", 0, 0)]),
            Track("right_pupil", frames: [Frame(0).Position(0, 0, 0), Frame(12).Position(0, 0, 0)])
        ]);
    }

    private PhaseDef Exit()
    {
        return Phase(durationTicks: 6, tracks: [
            Track("left_pupil", frames: [Frame(0).Position("-0.020", 0, 0), Frame(6).Position(0, 0, 0)]),
            Track("right_pupil", frames: [Frame(0).Position(0, 0, 0), Frame(6).Position(0, 0, 0)])
        ]);
    }
}
