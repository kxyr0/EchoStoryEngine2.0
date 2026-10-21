namespace JDH.Animation;

class LookLeftAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation("jdh:face/look_left_ticks",
            durationTicks: 24, end: "HOLD",
            tracks: [Track("left_pupil",
                    frames: [Frame(0, x: 0), Frame(6, x: "-0.025"),
                             Frame(18, x: "-0.025"), Frame(24, x: 0)])]);
    }
}
