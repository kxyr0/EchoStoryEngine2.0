namespace EchoDialogue.Sample;

class SampleLongOverflow : DialogueResource
{
    private void Init()
    {
        Id("sample:long_overflow");
        Start(LongLine);
        Alias("narrator", "sample:narrator", display: "Narrator");
    }

    private void LongLine()
    {
        Line("narrator", "This is a deliberately long dialogue line for testing fixed width wrapping, typewriter pacing, and overflow behavior near the edge of the screen. The panel must stay within the configured text width instead of stretching far away or crossing the screen boundary while the sentence is being revealed.");
    }
}
