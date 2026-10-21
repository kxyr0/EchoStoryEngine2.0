namespace JDH.Animation.Chapter1.Work;

face Molly("jdh:molly")
{
    Eyes
    {
        Sclera Left  { Position(-0.125, 0.06, 0); }
        Sclera Right { Position( 0.125, 0.06, 0); }

        Pupil Left   { Position(-0.09, 0, 0.01); }
        Pupil Right  { Position( 0.09, 0, 0.01); }

        Eyelid Upper { Position(0, -0.035, 0.02); TextureArea(8, 13, 8, 1); }
        Eyelid Lower { Position(0, 0.153, 0.02); TextureArea(8, 12, 8, 1); }
    }

    Brows
    {
        Pivot(-62, 0);

        Left  { Position(-0.125, -0.032, 0.03); }
        Right { Position( 0.124, -0.032, 0.03); Pivot(62, 0); }
    }

    Bone Glasses
    {
        Position(0, 0.06, 0.04);
        Texture("jdh:echo_animation/faces/molly/glasses");
        Size(6, 2);
    }

    Blink "eye_closure"
    {
        Travel(65, -61);
        Gaze(1, 1);
        Idle(true);
        PeriodTicks(60);
        CloseTicks(3);
        HoldTicks(3);
        OpenTicks(3);
    }
}
