namespace JDH.Animation.Chapter1.Player;

face PlayerBase("echo_content_player:player/base")
{
    Textures("jdh:echo_animation/faces/player");

    Eyes
    {
        Sclera Left  { Position(-0.125, 0, 0); }
        Sclera Right { Position( 0.125, 0, 0); }

        Pupil Left   { Position(-0.09, 0, 0.01); }
        Pupil Right  { Position( 0.09, 0, 0.01); }

        Eyelid Upper { Position(0, -0.035, 0.02); TextureArea(8, 13, 8, 1); }
        Eyelid Lower { Position(0, 0.09, 0.02); TextureArea(8, 12, 8, 1); }
    }

    Brows
    {
        Pivot(-62, 0);

        Left  { Position(-0.125, -0.032, 0.03); }
        Right { Position( 0.124, -0.032, 0.03); Pivot(62, 0); }
    }

    Blink "eye_closure"
    {
        Travel(35, -35);
        Gaze(1, 1);
        Idle(true);
        PeriodTicks(30);
        CloseTicks(2);
        HoldTicks(2);
        OpenTicks(2);
    }
}
