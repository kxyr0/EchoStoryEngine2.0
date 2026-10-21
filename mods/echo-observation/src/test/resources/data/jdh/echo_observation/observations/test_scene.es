class TestObservationScene : ObservationResource
{
    Observation BrokenWindow()
    {
        Focus(1.2);

        Text(
            "Стекло разбито изнутри."
        );

        Thought(
            "Кто-то разбил окно изнутри."
        );

        OnComplete()
        {
            Story.Start(
                "jdh:test_observation_completed"
            );
        }
    }

    Observation DoorLock()
    {
        Focus(1.0);

        Text(
            "На замке нет следов взлома."
        );
    }
}
