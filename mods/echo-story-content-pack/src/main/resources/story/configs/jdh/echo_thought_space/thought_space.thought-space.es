class Thoughts : ThoughtSpaceResource
{
    Style(
        zoomInitial: 1.0,
        zoomMin: 0.35,
        zoomMax: 3.0,
        zoomStep: 0.15,

        circleRadius: 30.0,
        nodeFontSize: 1.0,

        titleText: "ФОРМА МЫСЛИ",
        titleSize: 1.5,
        titleX: 0.5,
        titleY: 0.08
    );

    Thought TestBrokenGlass()
    {
        Text("На полу лежат осколки стекла.");
    }

    Thought TestOpenWindow()
    {
        Text("Окно открыто изнутри.");
    }

    Combination TestWindowConclusion()
    {
        From(
            "jdh:thought_space#test_broken_glass",
            "jdh:thought_space#test_open_window"
        );

        Thought("Похоже, окно разбили изнутри.");
    }
}
