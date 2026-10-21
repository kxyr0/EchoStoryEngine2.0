namespace JDH.Animation;

class WaveAnimation : AnimationResource
{
    public override AnimationDef Build()
    {
        return Animation("jdh:wave",
            channels: [Channel("body")],
            sources: [Source("body/idle")],
            entries: [Entry("body/idle", clip: "jdh:wave_enter", durationTicks: 6)],
            main: Main(clip: "jdh:wave_loop", mode: "LOOP_UNTIL_STOP", durationTicks: 24,
                exitPoints: [ExitPoint("neutral", progress: "0.0")]),
            exits: [Exit("neutral", clip: "jdh:wave_exit", durationTicks: 6)],
            target: "body/idle",
            recovery: Recovery("BOUNDED", checkpointIntervalTicks: 8),
            replay: "REJECT_IF_RUNNING");
    }
}

