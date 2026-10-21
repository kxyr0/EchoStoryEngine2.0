namespace JDH.Animation.Chapter1.Player;

class PlayerLookLeftAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation("jdh:player/look_left", rig: "echo_content_player:player/base", surfaces: [World, Portrait]);
    }
}

class World : FacialAnimationSurface3d
{
    private PhaseDef Enter()
    {
        return Phase(durationTicks: 6, tracks: [
            Track("left_pupil", frames: [Frame(0).Position(0, 0, 0), Frame(6).Position("-0.031", 0, 0)]),
            Track("right_pupil", frames: [Frame(0).Position(0, 0, 0), Frame(6).Position("-0.031", 0, 0)]),
            Track("upper_eyelids", frames: [Frame(0).Position(0, 0, 0), Frame(6).Position(0, "0.014", 0)]),
            Track("lower_eyelids", frames: [Frame(0).Position(0, 0, 0), Frame(6).Position(0, "-0.010", 0)]),
            Track("brows_left", frames: [Frame(0).Position(0, 0, 0).Rotation(0, 0, 0), Frame(6).Position("0.004", "-0.006", 0).Rotation(0, 0, "-4.0")]),
            Track("brows_right", frames: [Frame(0).Position(0, 0, 0).Rotation(0, 0, 0), Frame(6).Position("-0.004", "0.004", 0).Rotation(0, 0, "4.0")])
        ]);
    }

    private PhaseDef Stay()
    {
        return Phase(durationTicks: 12, end: "HOLD", tracks: [
            Track("left_pupil", frames: [Frame(0).Position("-0.031", 0, 0), Frame(12).Position("-0.031", 0, 0)]),
            Track("right_pupil", frames: [Frame(0).Position("-0.031", 0, 0), Frame(12).Position("-0.031", 0, 0)]),
            Track("upper_eyelids", frames: [Frame(0).Position(0, "0.014", 0), Frame(12).Position(0, "0.014", 0)]),
            Track("lower_eyelids", frames: [Frame(0).Position(0, "-0.010", 0), Frame(12).Position(0, "-0.010", 0)]),
            Track("brows_left", frames: [Frame(0).Position("0.004", "-0.006", 0).Rotation(0, 0, "-4.0"), Frame(12).Position("0.004", "-0.006", 0).Rotation(0, 0, "-4.0")]),
            Track("brows_right", frames: [Frame(0).Position("-0.004", "0.004", 0).Rotation(0, 0, "4.0"), Frame(12).Position("-0.004", "0.004", 0).Rotation(0, 0, "4.0")])
        ]);
    }

    private PhaseDef Exit()
    {
        return Phase(durationTicks: 6, tracks: [
            Track("left_pupil", frames: [Frame(0).Position("-0.031", 0, 0), Frame(6).Position(0, 0, 0)]),
            Track("right_pupil", frames: [Frame(0).Position("-0.031", 0, 0), Frame(6).Position(0, 0, 0)]),
            Track("upper_eyelids", frames: [Frame(0).Position(0, "0.014", 0), Frame(6).Position(0, 0, 0)]),
            Track("lower_eyelids", frames: [Frame(0).Position(0, "-0.010", 0), Frame(6).Position(0, 0, 0)]),
            Track("brows_left", frames: [Frame(0).Position("0.004", "-0.006", 0).Rotation(0, 0, "-4.0"), Frame(6).Position(0, 0, 0).Rotation(0, 0, 0)]),
            Track("brows_right", frames: [Frame(0).Position("-0.004", "0.004", 0).Rotation(0, 0, "4.0"), Frame(6).Position(0, 0, 0).Rotation(0, 0, 0)])
        ]);
    }
}

class Portrait : FacialAnimationSurfacePortrait
{
    private PhaseDef Enter()
    {
        return Phase(durationTicks: 6, tracks: [
            Track("left_pupil", frames: [Frame(0).Position(0, 0, 0), Frame(6).Position("-0.062", 0, 0)]),
            Track("right_pupil", frames: [Frame(0).Position(0, 0, 0), Frame(6).Position("-0.062", 0, 0)])
        ]);
    }

    private PhaseDef Stay()
    {
        return Phase(durationTicks: 12, end: "HOLD", tracks: [
            Track("left_pupil", frames: [Frame(0).Position("-0.062", 0, 0), Frame(12).Position("-0.062", 0, 0)]),
            Track("right_pupil", frames: [Frame(0).Position("-0.062", 0, 0), Frame(12).Position("-0.062", 0, 0)])
        ]);
    }

    private PhaseDef Exit()
    {
        return Phase(durationTicks: 6, tracks: [
            Track("left_pupil", frames: [Frame(0).Position("-0.062", 0, 0), Frame(6).Position(0, 0, 0)]),
            Track("right_pupil", frames: [Frame(0).Position("-0.062", 0, 0), Frame(6).Position(0, 0, 0)])
        ]);
    }
}
