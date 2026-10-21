class StudentMira : BackgroundNpcResource
{
    Profile("jdh:university_student");
    Home("jdh:dorm_mira");

    At("08:40")
    {
        GoTo("jdh:university_gate");
    }

    At("09:40")
    {
        GoTo("jdh:hallway_window");
    }

    At("09:50")
    {
        TalkWith(StudentAnton, "jdh:before_class", at: "jdh:hallway_window");
    }

    At("10:00")
    {
        GoTo("jdh:room_204");
    }

    At("11:30")
    {
        GoTo("jdh:cafeteria");
    }
}
