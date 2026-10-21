namespace JDH.Animation.Chapter1.Family.JohnFather;

class JohnFatherFullFaceHoldAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation("jdh:john_father/full_face_hold", rig: "jdh:john_father", surfaces: [World, Portrait]);
    }
}

class World : FacialAnimationSurface3d
{
    private PhaseDef Enter()
    {
        return Phase(durationTicks: 7, tracks: [
            Track("brows_left", frames: [Frame(0).Position(0, 0, 0).Rotation(0, 0, 0), Frame(7).Position("0.004", "-0.010", 0).Rotation(0, 0, "-4.0")]),
            Track("brows_right", frames: [Frame(0).Position(0, 0, 0).Rotation(0, 0, 0), Frame(7).Position("-0.004", "-0.010", 0).Rotation(0, 0, "4.0")]),
            Track("upper_eyelids", frames: [Frame(0).Position(0, 0, 0), Frame(7).Position(0, "0.020", 0)]),
            Track("lower_eyelids", frames: [Frame(0).Position(0, 0, 0), Frame(7).Position(0, "-0.020", 0)]),
            Track("left_pupil", frames: [Frame(0).Position(0, 0, 0), Frame(7).Position("-0.006", 0, 0)]),
            Track("right_pupil", frames: [Frame(0).Position(0, 0, 0), Frame(7).Position("0.006", 0, 0)])
        ]);
    }

    private PhaseDef Stay()
    {
        return Phase(durationTicks: 14, end: "HOLD", tracks: [
            Track("brows_left", frames: [Frame(0).Position("0.004", "-0.010", 0).Rotation(0, 0, "-4.0"), Frame(14).Position("0.004", "-0.010", 0).Rotation(0, 0, "-4.0")]),
            Track("brows_right", frames: [Frame(0).Position("-0.004", "-0.010", 0).Rotation(0, 0, "4.0"), Frame(14).Position("-0.004", "-0.010", 0).Rotation(0, 0, "4.0")]),
            Track("upper_eyelids", frames: [Frame(0).Position(0, "0.020", 0), Frame(14).Position(0, "0.020", 0)]),
            Track("lower_eyelids", frames: [Frame(0).Position(0, "-0.020", 0), Frame(14).Position(0, "-0.020", 0)]),
            Track("left_pupil", frames: [Frame(0).Position("-0.006", 0, 0), Frame(14).Position("-0.006", 0, 0)]),
            Track("right_pupil", frames: [Frame(0).Position("0.006", 0, 0), Frame(14).Position("0.006", 0, 0)])
        ]);
    }

    private PhaseDef Exit()
    {
        return Phase(durationTicks: 8, tracks: [
            Track("brows_left", frames: [Frame(0).Position("0.004", "-0.010", 0).Rotation(0, 0, "-4.0"), Frame(8).Position(0, 0, 0).Rotation(0, 0, 0)]),
            Track("brows_right", frames: [Frame(0).Position("-0.004", "-0.010", 0).Rotation(0, 0, "4.0"), Frame(8).Position(0, 0, 0).Rotation(0, 0, 0)]),
            Track("upper_eyelids", frames: [Frame(0).Position(0, "0.020", 0), Frame(8).Position(0, 0, 0)]),
            Track("lower_eyelids", frames: [Frame(0).Position(0, "-0.020", 0), Frame(8).Position(0, 0, 0)]),
            Track("left_pupil", frames: [Frame(0).Position("-0.006", 0, 0), Frame(8).Position(0, 0, 0)]),
            Track("right_pupil", frames: [Frame(0).Position("0.006", 0, 0), Frame(8).Position(0, 0, 0)])
        ]);
    }
}

class Portrait : FacialAnimationSurfacePortrait
{
    private PhaseDef Enter()
    {
        return Phase(durationTicks: 7, tracks: [
            Track("brows_left", frames: [Frame(0).Position(0, 0, 0).Rotation(0, 0, 0), Frame(7).Position("0.003", "-0.006", 0).Rotation(0, 0, "-3.0")]),
            Track("brows_right", frames: [Frame(0).Position(0, 0, 0).Rotation(0, 0, 0), Frame(7).Position("-0.003", "-0.006", 0).Rotation(0, 0, "3.0")]),
            Track("upper_eyelids", frames: [Frame(0).Position(0, 0, 0), Frame(7).Position(0, "0.012", 0)]),
            Track("lower_eyelids", frames: [Frame(0).Position(0, 0, 0), Frame(7).Position(0, "-0.010", 0)]),
            Track("left_pupil", frames: [Frame(0).Position(0, 0, 0), Frame(7).Position("-0.004", 0, 0)]),
            Track("right_pupil", frames: [Frame(0).Position(0, 0, 0), Frame(7).Position("0.004", 0, 0)])
        ]);
    }

    private PhaseDef Stay()
    {
        return Phase(durationTicks: 14, end: "HOLD", tracks: [
            Track("brows_left", frames: [Frame(0).Position("0.003", "-0.006", 0).Rotation(0, 0, "-3.0"), Frame(14).Position("0.003", "-0.006", 0).Rotation(0, 0, "-3.0")]),
            Track("brows_right", frames: [Frame(0).Position("-0.003", "-0.006", 0).Rotation(0, 0, "3.0"), Frame(14).Position("-0.003", "-0.006", 0).Rotation(0, 0, "3.0")]),
            Track("upper_eyelids", frames: [Frame(0).Position(0, "0.012", 0), Frame(14).Position(0, "0.012", 0)]),
            Track("lower_eyelids", frames: [Frame(0).Position(0, "-0.010", 0), Frame(14).Position(0, "-0.010", 0)]),
            Track("left_pupil", frames: [Frame(0).Position("-0.004", 0, 0), Frame(14).Position("-0.004", 0, 0)]),
            Track("right_pupil", frames: [Frame(0).Position("0.004", 0, 0), Frame(14).Position("0.004", 0, 0)])
        ]);
    }

    private PhaseDef Exit()
    {
        return Phase(durationTicks: 8, tracks: [
            Track("brows_left", frames: [Frame(0).Position("0.003", "-0.006", 0).Rotation(0, 0, "-3.0"), Frame(8).Position(0, 0, 0).Rotation(0, 0, 0)]),
            Track("brows_right", frames: [Frame(0).Position("-0.003", "-0.006", 0).Rotation(0, 0, "3.0"), Frame(8).Position(0, 0, 0).Rotation(0, 0, 0)]),
            Track("upper_eyelids", frames: [Frame(0).Position(0, "0.012", 0), Frame(8).Position(0, 0, 0)]),
            Track("lower_eyelids", frames: [Frame(0).Position(0, "-0.010", 0), Frame(8).Position(0, 0, 0)]),
            Track("left_pupil", frames: [Frame(0).Position("-0.004", 0, 0), Frame(8).Position(0, 0, 0)]),
            Track("right_pupil", frames: [Frame(0).Position("0.004", 0, 0), Frame(8).Position(0, 0, 0)])
        ]);
    }
}
