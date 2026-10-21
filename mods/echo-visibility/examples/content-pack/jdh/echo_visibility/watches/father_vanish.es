namespace JDH.Chapter1.Home.Visibility;

class FatherVanishWatch : VisibilityWatchResource
{
    private VisibilityWatchId FatherVanish = "jdh:home/father_vanish";
    private NpcId Father = "jdh:father";

    private void Init()
    {
        Id(FatherVanish);
        Target(Npc(Father));
        Viewpoint(ACTIVE_CAMERA);
        HiddenWhen(OUTSIDE_FRUSTUM_OR_OCCLUDED);
        Samples(NPC_HEAD, NPC_CHEST, NPC_PELVIS, NPC_FEET, BOUNDS_CORNERS);
        Occlusion(VISUAL_MULTI_RAY);
        RequireVisibleFirst();
        LostAfter(RenderedFrames(1));
        SeenAfter(RenderedFrames(2));
        EdgeMargin(Pixels(2));
        Concealment(HIDE_NPC_IMMEDIATELY);
        Evaluate(EVERY_RENDER_FRAME);
    }
}
