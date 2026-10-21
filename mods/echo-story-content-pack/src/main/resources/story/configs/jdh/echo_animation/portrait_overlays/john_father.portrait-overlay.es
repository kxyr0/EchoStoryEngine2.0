namespace JDH.Configs.EchoAnimation;

class JohnFatherPortraitOverlay : PortraitOverlayConfig
{
    public override PortraitOverlayDef Build()
    {
        return PortraitOverlay("jdh:john_father",
            centerX: "0.50",
            centerY: "0.49",
            offsetX: 0,
            offsetY: 0,
            facePixels: "4",
            layers: [
                Layer("sclera_left", placeX: "-0.062", placeY: "0.002", scaleX: "0.50", scaleY: "0.50"),
                Layer("sclera_right", placeX: "0.062", placeY: "0.002", scaleX: "0.50", scaleY: "0.50"),
                Layer("pupil_left", placeX: "-0.062", placeY: "0.002", scaleX: "0.50", scaleY: "0.50"),
                Layer("pupil_right", placeX: "0.0309", placeY: "0.002", scaleX: "0.50", scaleY: "0.50"),
                Layer("upper_eyelid_left", placeX: "-0.062", placeY: "-0.034", scaleX: "0.50", scaleY: "0.50"),
                Layer("upper_eyelid_right", placeX: "0.062", placeY: "-0.034", scaleX: "0.50", scaleY: "0.50"),
                Layer("lower_eyelid_left", placeX: "-0.062", placeY: "0.034", scaleX: "0.50", scaleY: "0.50"),
                Layer("lower_eyelid_right", placeX: "0.062", placeY: "0.034", scaleX: "0.50", scaleY: "0.50"),
                Layer("brow_left", placeX: "-0.062", placeY: "-0.029", scaleX: "0.50", scaleY: "0.50"),
                Layer("brow_right", placeX: "0.062", placeY: "-0.029", scaleX: "0.50", scaleY: "0.50")
            ],
            idle: [
                Blink "eye_closure"
                {
                    Travel(20, -20);
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
