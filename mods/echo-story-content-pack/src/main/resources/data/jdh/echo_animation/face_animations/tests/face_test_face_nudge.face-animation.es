namespace JDH.Animation.Tests.Face;

class FaceTestFaceNudgeAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation("jdh:face_test/face_nudge", surfaces: [World, Portrait]);
    }
}

class World : FacialAnimationSurface3d
{
    private PhaseDef Enter()
    {
        return Phase(durationTicks: 5, tracks: [
            Track("face_root", frames: [Frame(0).Position(0, 0, 0), Frame(5).Position("0.025", "0.015", 0)])
        ]);
    }

    private PhaseDef Stay()
    {
        return Phase(durationTicks: 10, end: "HOLD", tracks: [
            Track("face_root", frames: [Frame(0).Position("0.025", "0.015", 0), Frame(5).Position("-0.025", "-0.015", 0), Frame(10).Position("0.015", 0, 0)])
        ]);
    }

    private PhaseDef Exit()
    {
        return Phase(durationTicks: 5, tracks: [
            Track("face_root", frames: [Frame(0).Position("0.015", 0, 0), Frame(5).Position(0, 0, 0)])
        ]);
    }
}

class Portrait : FacialAnimationSurfacePortrait
{
    private PhaseDef Enter()
    {
        return Phase(durationTicks: 5, tracks: [
            Track("face_root", frames: [Frame(0).Position(0, 0, 0), Frame(5).Position("0.012", "0.008", 0)])
        ]);
    }

    private PhaseDef Stay()
    {
        return Phase(durationTicks: 10, end: "HOLD", tracks: [
            Track("face_root", frames: [Frame(0).Position("0.012", "0.008", 0), Frame(5).Position("-0.012", "-0.008", 0), Frame(10).Position("0.008", 0, 0)])
        ]);
    }

    private PhaseDef Exit()
    {
        return Phase(durationTicks: 5, tracks: [
            Track("face_root", frames: [Frame(0).Position("0.008", 0, 0), Frame(5).Position(0, 0, 0)])
        ]);
    }
}
