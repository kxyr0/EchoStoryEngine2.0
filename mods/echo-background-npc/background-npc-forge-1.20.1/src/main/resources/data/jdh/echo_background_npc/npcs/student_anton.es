class StudentAnton : BackgroundNpcResource
{
    Profile("jdh:university_student");
    Home("jdh:dorm_anton");

    At("08:45")
    {
        GoTo("jdh:university_gate");
    }

    At("09:40")
    {
        GoTo("jdh:hallway_window");
    }

    At("10:00")
    {
        GoTo("jdh:room_204");
    }
}
