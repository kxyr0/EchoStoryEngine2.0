namespace JDH.Configs.EchoDialogue;

class DialogueOverlayStyle : DialogueOverlayStyleConfig
{
    public override DialogueOverlayStyleDef Build()
    {
        return DialogueOverlayStyle(
            textScale: "1.0",
            textShadow: true,
            textAppearDelayMs: 80,
            typewriterMsPerCharacter: 34,
            typewriterMinDurationMs: 450,
            typewriterMaxDurationMs: 5000,

            anchorX: "0.5",
            anchorY: "0.5",
            anchorOffsetX: 0,
            anchorOffsetY: 20,
            crosshairPaddingY: 10,
            screenMarginX: 8,
            screenMarginY: 8,

            minTextWidth: 30,
            maxTextWidth: 256,
            paddingX: 8,
            paddingY: 4,
            lineGap: 2,
            choiceSectionGap: 7,
            choicePaddingX: 6,
            choicePaddingY: 4,
            choiceGap: 4,
            visualSize: 28,
            visualGap: 8,
            visualTextOffsetY: 1,

            backgroundColor: "0xB0101010",
            choiceBackgroundColor: "0x80181818",
            choiceBorderColor: "0x806A6A6A",
            speakerColor: "0xFF6A00",
            textColor: "0xFFFFFF",
            choiceNumberColor: "0xFF6A00",
            choiceTextColor: "0xFFE6E6E6"
        );
    }
}
