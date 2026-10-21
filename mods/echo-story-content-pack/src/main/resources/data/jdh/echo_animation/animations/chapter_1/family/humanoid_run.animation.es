namespace JDH.Animation;

class HumanoidRunAnimation : AnimationResource
{
    public override void Build()
    {
        Id("jdh:humanoid_run");

        var body = Channel("body");
        var idle = State(body, "idle");
        var run = State(body, "run");
        var motion = Clip("jdh:humanoid_run");

        Enter(run, from: idle, play: motion.Segment("idle_to_run"));
        Loop(run, play: motion.Segment("run_loop"));
        Exit(run, to: idle, play: motion.Segment("run_to_idle"), at: motion.Point("neutral"));
        Fallback(body, Crossfade(3));
        Recovery(Bounded, checkpointEvery: 3);
        Replay(RejectIfRunning);
    }
}
