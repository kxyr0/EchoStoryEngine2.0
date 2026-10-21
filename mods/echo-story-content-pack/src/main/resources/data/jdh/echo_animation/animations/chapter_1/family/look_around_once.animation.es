namespace JDH.Animation;

class LookAroundOnceAnimation : AnimationResource
{
    public override AnimationDef Build()
    {
        return Animation("jdh:look_around_once",
            channels: [Channel("body")],
            sources: [Source("body/idle")],
            entries: [Entry("body/idle", clip: "jdh:look_around_enter", durationTicks: 4)],
            main: Main(clip: "jdh:look_around_main", mode: "ONCE", durationTicks: 48,
                exitPoints: [ExitPoint("complete", progress: "1.0")]),
            exits: [Exit("complete", clip: "jdh:look_around_exit", durationTicks: 4)],
            target: "body/idle",
            recovery: Recovery("BOUNDED", checkpointIntervalTicks: 6),
            replay: "REJECT_IF_RUNNING");
    }
}

