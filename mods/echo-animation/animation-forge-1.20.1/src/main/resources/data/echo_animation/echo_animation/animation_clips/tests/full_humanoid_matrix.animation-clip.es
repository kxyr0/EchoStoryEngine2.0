namespace EchoAnimation.Tests;

class FullHumanoidMatrixClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("echo_animation:full_humanoid_matrix_test",
            rig: "echo_animation:tests/full_humanoid",
            tracks: [
                Track("left_forearm", frames: [
                    Frame("0.0").Matrix(
                        "1.0", "0.0", "0.0", "0.0",
                        "0.0", "1.0", "0.0", "4.0",
                        "0.0", "0.0", "1.0", "0.0",
                        "0.0", "0.0", "0.0", "1.0"),
                    Frame("0.25", interpolation: "SMOOTH").Matrix(
                        "1.0", "0.20", "0.0", "0.0",
                        "0.0", "0.5735764364", "-0.8191520443", "4.0",
                        "0.0", "0.8191520443", "0.5735764364", "0.0",
                        "0.0", "0.0", "0.0", "1.0"),
                    Frame("0.5", interpolation: "SMOOTH").Matrix(
                        "1.0", "0.0", "0.0", "0.0",
                        "0.0", "1.0", "0.0", "4.0",
                        "0.0", "0.0", "1.0", "0.0",
                        "0.0", "0.0", "0.0", "1.0"),
                    Frame("0.75", interpolation: "SMOOTH").Matrix(
                        "1.0", "-0.20", "0.0", "0.0",
                        "0.0", "0.5735764364", "0.8191520443", "4.0",
                        "0.0", "-0.8191520443", "0.5735764364", "0.0",
                        "0.0", "0.0", "0.0", "1.0"),
                    Frame("1.0", interpolation: "SMOOTH").Matrix(
                        "1.0", "0.0", "0.0", "0.0",
                        "0.0", "1.0", "0.0", "4.0",
                        "0.0", "0.0", "1.0", "0.0",
                        "0.0", "0.0", "0.0", "1.0")
                ])
            ]);
    }
}
