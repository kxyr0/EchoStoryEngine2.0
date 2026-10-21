namespace EchoAnimation.Tests;

class FullHumanoidRig : AnimationRigResource
{
    public override AnimationRigDef Build()
    {
        return AnimationRig("echo_animation:tests/full_humanoid",
            kind: "SKINNED_MESH", skin: "CLASSIC",
            bones: [
                Bone("root"),
                Bone("body", parent: "root"),
                Bone("head", parent: "body"),
                Bone("left_shoulder", parent: "body", x: "5.0", y: "2.0"),
                Bone("left_upper_arm", parent: "left_shoulder"),
                Bone("left_forearm", parent: "left_upper_arm", y: "4.0"),
                Bone("right_shoulder", parent: "body", x: "-5.0", y: "2.0"),
                Bone("right_upper_arm", parent: "right_shoulder"),
                Bone("right_forearm", parent: "right_upper_arm", y: "4.0"),
                Bone("left_thigh", parent: "root", x: "2.0", y: "12.0"),
                Bone("left_shin", parent: "left_thigh", y: "6.0"),
                Bone("right_thigh", parent: "root", x: "-2.0", y: "12.0"),
                Bone("right_shin", parent: "right_thigh", y: "6.0")
            ],
            sockets: [
                Socket("main_hand", bone: "right_forearm", y: "6.0"),
                Socket("off_hand", bone: "left_forearm", y: "6.0")
            ]);
    }
}
