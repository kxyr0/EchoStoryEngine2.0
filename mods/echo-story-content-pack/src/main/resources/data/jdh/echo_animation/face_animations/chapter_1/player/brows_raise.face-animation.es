namespace JDH.Animation.Chapter1.Player;

class PlayerBrowsRaiseAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation("jdh:player/brows_raise", rig: "echo_content_player:player/base", surfaces: [World, Portrait]);
    }
}

class World : FacialAnimationSurface3d
{
    private PhaseDef Enter()
    {
        return Phase(durationTicks: 5, tracks: [
            Track("brows_left", frames: [Frame(0).Position(0, 0, 0), Frame(5).Position(0, "-0.018", 0)]),
            Track("brows_right", frames: [Frame(0).Position(0, 0, 0), Frame(5).Position(0, "-0.018", 0)])
        ]);
    }

    private PhaseDef Stay()
    {
        return Phase(durationTicks: 12, end: "HOLD", tracks: [
            Track("brows_left", frames: [Frame(0).Position(0, "-0.018", 0), Frame(12).Position(0, "-0.018", 0)]),
            Track("brows_right", frames: [Frame(0).Position(0, "-0.018", 0), Frame(12).Position(0, "-0.018", 0)])
        ]);
    }

    private PhaseDef Exit()
    {
        return Phase(durationTicks: 7, tracks: [
            Track("brows_left", frames: [Frame(0).Position(0, "-0.018", 0), Frame(7).Position(0, 0, 0)]),
            Track("brows_right", frames: [Frame(0).Position(0, "-0.018", 0), Frame(7).Position(0, 0, 0)])
        ]);
    }
}

class Portrait : FacialAnimationSurfacePortrait
{
    private PhaseDef Enter()
    {
        return Phase(durationTicks: 5, tracks: [
            Track("brows_left", frames: [Frame(0).Position(0, 0, 0), Frame(5).Position(0, "-0.010", 0)]),
            Track("brows_right", frames: [Frame(0).Position(0, 0, 0), Frame(5).Position(0, "-0.010", 0)])
        ]);
    }

    private PhaseDef Stay()
    {
        return Phase(durationTicks: 12, end: "HOLD", tracks: [
            Track("brows_left", frames: [Frame(0).Position(0, "-0.010", 0), Frame(12).Position(0, "-0.010", 0)]),
            Track("brows_right", frames: [Frame(0).Position(0, "-0.010", 0), Frame(12).Position(0, "-0.010", 0)])
        ]);
    }

    private PhaseDef Exit()
    {
        return Phase(durationTicks: 7, tracks: [
            Track("brows_left", frames: [Frame(0).Position(0, "-0.010", 0), Frame(7).Position(0, 0, 0)]),
            Track("brows_right", frames: [Frame(0).Position(0, "-0.010", 0), Frame(7).Position(0, 0, 0)])
        ]);
    }
}
