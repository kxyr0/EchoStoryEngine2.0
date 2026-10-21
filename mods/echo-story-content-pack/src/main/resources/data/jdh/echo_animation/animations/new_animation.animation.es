namespace JDH.Animation;

class NewAnimationAnimation : AnimationResource
{
    public override AnimationDef Build()
    {
        return Animation("jdh:new_animation",
            channels: [Channel("body")],
            sources: [Source("body/idle")],
            entries: [Entry("body/idle", clip: "jdh:new_animation_enter", durationTicks: 4)],
            main: Main(clip: "jdh:new_animation_main", mode: "ONCE", durationTicks: 166,
                exitPoints: [ExitPoint("complete", progress: "1.0")]),
            exits: [Exit("complete", clip: "jdh:new_animation_exit", durationTicks: 4)],
            target: "body/idle",
            recovery: Recovery("BOUNDED", checkpointIntervalTicks: 4),
            replay: "REJECT_IF_RUNNING");
    }
}
