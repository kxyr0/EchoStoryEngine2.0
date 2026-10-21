namespace JDH.Animation.Chapter1.Family.JohnFather;

class JohnFatherAngryBrowsAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation("jdh:john_father/angry_brows", rig: "jdh:john_father", surfaces: [World, Portrait]);
    }
}

class World : FacialAnimationSurface3d
{
    private PhaseDef Enter()
    {
        return Phase(durationTicks: 5, tracks: [
            Track("brows_left", frames:
            [
                Frame(0).Position(0, 0, 0).Rotation(0, 0, 0),
                Frame(5).Position(0.020, 0.018, 0).Rotation(0, 0, 18)
            ]),
            Track("brows_right", frames:
            [
                Frame(0).Position(0, 0, 0).Rotation(0, 0, 0),
                Frame(5).Position(-0.020, 0.018, 0).Rotation(0, 0, -18)
            ])
        ]);
    }

    private PhaseDef Stay()
    {
        return Phase(durationTicks: 12, end: "HOLD", tracks: [
            Track("brows_left", frames:
            [
                Frame(0).Position(0.020, 0.018, 0).Rotation(0, 0, 18),
                Frame(12).Position(0.020, 0.018, 0).Rotation(0, 0, 18)
            ]),
            Track("brows_right", frames:
            [
                Frame(0).Position(-0.020, 0.018, 0).Rotation(0, 0, -18),
                Frame(12).Position(-0.020, 0.018, 0).Rotation(0, 0, -18)
            ])
        ]);
    }

    private PhaseDef Exit()
    {
        return Phase(durationTicks: 7, tracks: [
            Track("brows_left", frames:
            [
                Frame(0).Position(0.020, 0.018, 0).Rotation(0, 0, 18),
                Frame(7).Position(0, 0, 0).Rotation(0, 0, 0)
            ]),
            Track("brows_right", frames:
            [
                Frame(0).Position(-0.020, 0.018, 0).Rotation(0, 0, -18),
                Frame(7).Position(0, 0, 0).Rotation(0, 0, 0)
            ])
        ]);
    }
}

class Portrait : FacialAnimationSurfacePortrait
{
    private PhaseDef Enter()
    {
        return Phase(durationTicks: 5, tracks: [
            Track("brows_left", frames:
            [
                Frame(0).Position(0, 0, 0).Rotation(0, 0, 0),
                Frame(5).Position(0.010, 0.008, 0).Rotation(0, 0, 10)
            ]),
            Track("brows_right", frames:
            [
                Frame(0).Position(0, 0, 0).Rotation(0, 0, 0),
                Frame(5).Position(-0.010, 0.008, 0).Rotation(0, 0, -10)
            ])
        ]);
    }

    private PhaseDef Stay()
    {
        return Phase(durationTicks: 12, end: "HOLD", tracks: [
            Track("brows_left", frames:
            [
                Frame(0).Position(0.010, 0.008, 0).Rotation(0, 0, 10),
                Frame(12).Position(0.010, 0.008, 0).Rotation(0, 0, 10)
            ]),
            Track("brows_right", frames:
            [
                Frame(0).Position(-0.010, 0.008, 0).Rotation(0, 0, -10),
                Frame(12).Position(-0.010, 0.008, 0).Rotation(0, 0, -10)
            ])
        ]);
    }

    private PhaseDef Exit()
    {
        return Phase(durationTicks: 7, tracks: [
            Track("brows_left", frames:
            [
                Frame(0).Position(0.010, 0.008, 0).Rotation(0, 0, 10),
                Frame(7).Position(0, 0, 0).Rotation(0, 0, 0)
            ]),
            Track("brows_right", frames:
            [
                Frame(0).Position(-0.010, 0.008, 0).Rotation(0, 0, -10),
                Frame(7).Position(0, 0, 0).Rotation(0, 0, 0)
            ])
        ]);
    }
}
