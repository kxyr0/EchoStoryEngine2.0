package io.github.kxyr0.echoanimation.forge.gametest;

/**
 * EchoScript sources used by Forge GameTests.
 */
final class ForgeAnimationGameTestSources {
    private ForgeAnimationGameTestSources() {
    }

    static String faceRig() {
        return """
                namespace JDH.GameTest;

                class GameTestRig : FaceRigResource
                {
                    public override FaceRigDef Build()
                    {
                        return FaceRig("sample:father",
                            bones: [Bone("face_root"), Bone("upper_eyelids", parent: "face_root")],
                            layers: [Layer("glasses", bone: "face_root", texture: "sample:face/glasses",
                                    role: "GLASSES", order: 50, x: 0, y: 0, w: 8, h: 8)],
                            controls: [BlinkControl("eye_closure", upperTravel: 1000, lowerTravel: 1000)]
                        );
                    }
                }
                """;
    }

    static String facePose() {
        return """
                namespace JDH.GameTest;

                class GameTestPose : FacePoseResource
                {
                    public override FacePoseDef Build()
                    {
                        return FacePose("sample:father/neutral", rig: "sample:father",
                            bones: [BoneDelta("upper_eyelids", y: 0)],
                            controls: [ControlDelta("eye_closure", value: 0)]
                        );
                    }
                }
                """;
    }

    static String facialAnimation() {
        return """
                namespace JDH.GameTest;

                class GameTestBlink : FacialAnimationResource
                {
                    public override FacialAnimationDef Build()
                    {
                        return FacialAnimation("sample:shared/blink", rig: "sample:father",
                            durationMillis: 1000, end: "HOLD",
                            tracks: [Track("upper_eyelids", frames: [
                                Frame(0, y: 0),
                                Frame(1000, y: 220, interpolation: "LINEAR")
                            ])]
                        );
                    }
                }
                """;
    }

    static String invalidRig() {
        return """
                namespace JDH.GameTest;

                class BrokenRig : FaceRigResource
                {
                    public override FaceRigDef Build()
                    {
                        return FaceRig("sample:broken", unexpected: 1, bones: [Bone("face_root")]);
                    }
                }
                """;
    }

    static String bodyRig() {
        return """
                namespace JDH.GameTest;

                class BodyRig : AnimationRigResource
                {
                    public override AnimationRigDef Build()
                    {
                        return AnimationRig("jdh:humanoid", skin: "CLASSIC");
                    }
                }
                """;
    }

    static String bodyClip(String boneId, int forearmRoll) {
        return """
                namespace JDH.GameTest;

                class WaveClip : AnimationClipResource
                {
                    public override AnimationClipDef Build()
                    {
                        return AnimationClip("jdh:wave", rig: "jdh:humanoid",
                            tracks: [Track("%s",
                                frames: [Frame("0.0", rotZ: 0),
                                         Frame("1.0", rotZ: %d)])]);
                    }
                }
                """.formatted(boneId, forearmRoll);
    }
}
