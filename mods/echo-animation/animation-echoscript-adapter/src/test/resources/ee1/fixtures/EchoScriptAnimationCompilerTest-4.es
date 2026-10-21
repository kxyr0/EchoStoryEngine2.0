namespace JDH.Animation;

class BlinkAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation("jdh:shared/blink", rig: "jdh:john_father",
            durationMillis: 1000, end: "HOLD",
            tracks: [Track("upper_eyelids",
                    frames: [Frame(0, y: 0), Frame(1000, y: 220)])]);
    }
}
