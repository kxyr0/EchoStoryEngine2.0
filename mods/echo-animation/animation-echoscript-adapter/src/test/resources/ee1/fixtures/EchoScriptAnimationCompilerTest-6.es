namespace JDH.Animation;

class LookLeftAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation("jdh:face/look_left",
            durationMillis: 900, end: "HOLD",
            tracks: [Track("left_pupil",
                    frames: [Frame(0, x: 0), Frame(900, x: "-0.025")])]);
    }
}
