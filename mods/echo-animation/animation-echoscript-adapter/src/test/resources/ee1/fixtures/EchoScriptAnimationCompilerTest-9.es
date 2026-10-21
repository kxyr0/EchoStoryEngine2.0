namespace JDH.Animation;

class LookLeftAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation("jdh:face/look_left_split",
            surfaces: [
                Surface("3d", rig: "jdh:john_father",
                    enter: Phase(durationTicks: 4,
                        tracks: [Track("left_pupil",
                            frames: [Frame(0, x: 0), Frame(4, x: "-0.020")])]),
                    stay: Phase(durationTicks: 12, end: "HOLD",
                        tracks: [Track("left_pupil",
                            frames: [Frame(0, x: "-0.020"), Frame(12, x: "-0.020")])]),
                    exit: Phase(durationTicks: 4,
                        tracks: [Track("left_pupil",
                            frames: [Frame(0, x: "-0.020"), Frame(4, x: 0)])])),
                Surface("portrait",
                    enter: Phase(durationTicks: 4,
                        tracks: [Track("left_pupil",
                            frames: [Frame(0, x: 0), Frame(4, x: "-0.031")])]),
                    stay: Phase(durationTicks: 12,
                        tracks: [Track("left_pupil",
                            frames: [Frame(0, x: "-0.031"), Frame(12, x: "-0.031")])]),
                    exit: Phase(durationTicks: 4,
                        tracks: [Track("left_pupil",
                            frames: [Frame(0, x: "-0.031"), Frame(4, x: 0)])]))
            ]);
    }
}
