namespace JDH.Chapter1.Home.Visibility;

class KitchenEnvironmentWatch : VisibilityWatchResource
{
    private VisibilityWatchId Kitchen = "jdh:home/kitchen_change";

    private void Init()
    {
        Id(Kitchen);
        Target(Box(
            dimension: "minecraft:overworld",
            min: Position(-8.5, 60, -4),
            max: Position(8, 72, 12)
        ));
        Viewpoint(ACTIVE_CAMERA);
        HiddenWhen(OUTSIDE_FRUSTUM_OR_OCCLUDED);
        Samples(BOX_CENTER, BOX_CORNERS, BOX_FACE_CENTERS);
        Occlusion(VISUAL_MULTI_RAY);
        RequireVisibleFirst();
        LostAfter(RenderedFrames(1));
        SeenAfter(RenderedFrames(2));
        EdgeMargin(Pixels(2));
        Evaluate(EVERY_CLIENT_TICK);
    }
}
