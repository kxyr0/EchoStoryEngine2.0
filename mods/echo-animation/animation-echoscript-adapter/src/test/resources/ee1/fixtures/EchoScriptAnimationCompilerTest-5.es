namespace JDH.Animation;

class SleepAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation("jdh:sasha/sleep", rig: "jdh:sasha",
            durationTicks: 20,
            suppressIdleBlink: true,
            tracks: [Track("upper_eyelids",
                    frames: [Frame(0, y: 0), Frame(20, y: 0)])]);
    }
}
