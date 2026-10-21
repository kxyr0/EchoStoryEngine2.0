namespace JDH.Animation;

class HumanoidWalkAnimation : AnimationResource
{
    public override AnimationDef Build()
    {
        return Animation("jdh:humanoid_walk",
            channels: [Channel("body")],
            sources: [Source("body/idle")],
            entries: [Entry("body/idle", clip: "jdh:humanoid_walk_enter", durationTicks: 4)],
            main: Main(clip: "jdh:humanoid_walk_loop", mode: "LOOP_UNTIL_STOP", durationTicks: 16,
                exitPoints: [ExitPoint("neutral", progress: "0.0")]),
            exits: [Exit("neutral", clip: "jdh:humanoid_walk_exit", durationTicks: 5)],
            target: "body/idle",
            recovery: Recovery("BOUNDED", checkpointIntervalTicks: 4),
            replay: "REJECT_IF_RUNNING");
    }
}

