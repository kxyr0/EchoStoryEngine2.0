namespace JDH.Animation;

class DirectAnimation : AnimationResource
{
    public override AnimationDef Build()
    {
        return Animation("%s",
            channels: [Channel("body")],
            sources: [Source("body/idle")],
            entries: [],
            main: Main(clip: "%s", mode: "%s", durationTicks: 12,
                exitPoints: []),
            exits: [],
            target: "body/idle",
            recovery: Recovery("BOUNDED", checkpointIntervalTicks: 4),
            replay: "REJECT_IF_RUNNING");
    }
}
