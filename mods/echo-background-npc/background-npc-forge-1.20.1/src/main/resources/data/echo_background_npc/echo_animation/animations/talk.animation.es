namespace EchoBackgroundNpc.Animation;

class AmbientTalkAnimation : AnimationResource
{
    public override AnimationDef Build()
    {
        return Animation("echo_background_npc:talk",
            channels: [Channel("body")],
            sources: [Source("body/idle")],
            entries: [],
            main: Main(clip: "echo_background_npc:talk_loop", mode: "LOOP_UNTIL_STOP", durationTicks: 28,
                exitPoints: []),
            exits: [],
            target: "body/idle",
            recovery: Recovery("BOUNDED", checkpointIntervalTicks: 7),
            replay: "REJECT_IF_RUNNING");
    }
}
