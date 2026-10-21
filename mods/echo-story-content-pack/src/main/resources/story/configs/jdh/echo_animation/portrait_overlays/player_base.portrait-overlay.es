namespace JDH.Configs.EchoAnimation;

class PlayerBasePortraitOverlay : PortraitOverlayConfig
{
    public override PortraitOverlayDef Build()
    {
        return PortraitOverlay("echo_content_player:player/base",
            centerX: "0.5",
            centerY: "0.49",
            offsetX: 0,
            offsetY: 0,
            facePixels: "8",
            layers: [
                Layer("sclera_left", placeX: "-0.125", placeY: "0.0637", scaleX: "1", scaleY: "1"),
                Layer("sclera_right", placeX: "0.125", placeY: "0.0637", scaleX: "1", scaleY: "1"),
                Layer("pupil_left", placeX: "-0.1", placeY: "0.063", scaleX: "1", scaleY: "1"),
                Layer("pupil_right", placeX: "0.089", placeY: "0.063", scaleX: "1", scaleY: "1"),
                Layer("upper_eyelid_left", placeX: "-0.125", placeY: "0", scaleX: "1", scaleY: "1"),
                Layer("upper_eyelid_right", placeX: "0.125", placeY: "0", scaleX: "1", scaleY: "1"),
                Layer("lower_eyelid_left", placeX: "-0.125", placeY: "0.125", scaleX: "1", scaleY: "1"),
                Layer("lower_eyelid_right", placeX: "0.125", placeY: "0.125", scaleX: "1", scaleY: "1"),
                Layer("brow_left", placeX: "-0.125", placeY: "0.001", scaleX: "1", scaleY: "1"),
                Layer("brow_right", placeX: "0.125", placeY: "0.001", scaleX: "1", scaleY: "1")
            ],
            idle: [
                Blink "eye_closure"
                {
                    Travel(32, -32);
                    Gaze(1, 1);
                    Idle(true);
                    PeriodTicks(50);
                    CloseTicks(2);
                    HoldTicks(2);
                    OpenTicks(2);
                }
            ]
        );
    }
}
