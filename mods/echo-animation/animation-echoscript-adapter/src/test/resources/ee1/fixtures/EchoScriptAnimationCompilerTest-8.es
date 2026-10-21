namespace JDH.Animation;

class FluentFrameAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation("jdh:face/fluent",
            durationTicks: 6, end: "HOLD",
            tracks: [Track("brows_left",
                    frames: [
                        Frame(0).Position(0, 0, 0).Rotation(0, 0, 0),
                        Frame(6).Position(-0.031, 0.004, 0).Rotation(0, 0, -18).Scale(1.25, 1, 1)
                    ])]);
    }
}
