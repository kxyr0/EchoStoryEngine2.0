namespace JDH.Animation;

class KneeBendAnimation : AnimationResource
{
    public override AnimationDef Build()
    {
        return Animation("jdh:knee_bend",
            channels: [Channel("body")],
            sources: [Source("body/idle")],
            entries: [Entry("body/idle", clip: "jdh:knee_bend_enter", durationTicks: 4)],
            main: Main(clip: "jdh:knee_bend_main", mode: "ONCE", durationTicks: 32,
                exitPoints: [ExitPoint("complete", progress: "1.0")]),
            exits: [Exit("complete", clip: "jdh:knee_bend_exit", durationTicks: 16)],
            target: "body/idle",
            recovery: Recovery("BOUNDED", checkpointIntervalTicks: 4),
            replay: "REJECT_IF_RUNNING");
    }
}

