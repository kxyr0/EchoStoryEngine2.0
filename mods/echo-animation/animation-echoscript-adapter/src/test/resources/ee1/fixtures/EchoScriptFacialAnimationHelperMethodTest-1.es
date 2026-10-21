namespace JDH.Animation;

class LookLeftAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation("jdh:face/look_left_helpers", surfaces: [World, Portrait]);
    }
}

class World : FacialAnimationSurface3d
{
    private FaceRigId Rig()
    {
        return FaceRig("jdh:john_father");
    }

    private PhaseDef Enter()
    {
        return Phase(durationTicks: 6, tracks: [
            Track("left_pupil", frames: [Frame(0, x: 0), Frame(6, x: "-0.031")])
        ]);
    }

    private PhaseDef Stay()
    {
        return Phase(durationTicks: 12, end: "HOLD", tracks: [
            Track("left_pupil", frames: [Frame(0, x: "-0.031"), Frame(12, x: "-0.031")])
        ]);
    }

    private PhaseDef Exit()
    {
        return Phase(durationTicks: 6, tracks: [
            Track("left_pupil", frames: [Frame(0, x: "-0.031"), Frame(6, x: 0)])
        ]);
    }
}

class Portrait : FacialAnimationSurfacePortrait
{
    private PhaseDef Enter()
    {
        return Phase(durationTicks: 6, tracks: [
            Track("left_pupil", frames: [Frame(0, x: 0), Frame(6, x: "-0.020")])
        ]);
    }

    private PhaseDef Stay()
    {
        return Phase(durationTicks: 12, end: "HOLD", tracks: [
            Track("left_pupil", frames: [Frame(0, x: "-0.020"), Frame(12, x: "-0.020")])
        ]);
    }

    private PhaseDef Exit()
    {
        return Phase(durationTicks: 6, tracks: [
            Track("left_pupil", frames: [Frame(0, x: "-0.020"), Frame(6, x: 0)])
        ]);
    }
}
