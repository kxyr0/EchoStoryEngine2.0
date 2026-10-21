namespace JDH.Animation.Tests.Face;

class FaceTestAngryBrowsAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation("jdh:face_test/angry_brows", surfaces: [World, Portrait]);
    }
}

class World : FacialAnimationSurface3d
{
    private PhaseDef Enter()
    {
        return Phase(durationTicks: 5, tracks: [
            Track("brows_left", frames: [Frame(0).Position(0, 0, 0), Frame(5).Position("0.012", "0.012", 0)]),
            Track("brows_right", frames: [Frame(0).Position(0, 0, 0), Frame(5).Position("-0.012", "0.012", 0)])
        ]);
    }

    private PhaseDef Stay()
    {
        return Phase(durationTicks: 12, end: "HOLD", tracks: [
            Track("brows_left", frames: [Frame(0).Position("0.012", "0.012", 0), Frame(12).Position("0.012", "0.012", 0)]),
            Track("brows_right", frames: [Frame(0).Position("-0.012", "0.012", 0), Frame(12).Position("-0.012", "0.012", 0)])
        ]);
    }

    private PhaseDef Exit()
    {
        return Phase(durationTicks: 7, tracks: [
            Track("brows_left", frames: [Frame(0).Position("0.012", "0.012", 0), Frame(7).Position(0, 0, 0)]),
            Track("brows_right", frames: [Frame(0).Position("-0.012", "0.012", 0), Frame(7).Position(0, 0, 0)])
        ]);
    }
}

class Portrait : FacialAnimationSurfacePortrait
{
    private PhaseDef Enter()
    {
        return Phase(durationTicks: 5, tracks: [
            Track("brows_left", frames: [Frame(0).Position(0, 0, 0), Frame(5).Position("0.006", "0.006", 0)]),
            Track("brows_right", frames: [Frame(0).Position(0, 0, 0), Frame(5).Position("-0.006", "0.006", 0)])
        ]);
    }

    private PhaseDef Stay()
    {
        return Phase(durationTicks: 12, end: "HOLD", tracks: [
            Track("brows_left", frames: [Frame(0).Position("0.006", "0.006", 0), Frame(12).Position("0.006", "0.006", 0)]),
            Track("brows_right", frames: [Frame(0).Position("-0.006", "0.006", 0), Frame(12).Position("-0.006", "0.006", 0)])
        ]);
    }

    private PhaseDef Exit()
    {
        return Phase(durationTicks: 7, tracks: [
            Track("brows_left", frames: [Frame(0).Position("0.006", "0.006", 0), Frame(7).Position(0, 0, 0)]),
            Track("brows_right", frames: [Frame(0).Position("-0.006", "0.006", 0), Frame(7).Position(0, 0, 0)])
        ]);
    }
}
