package io.github.kxyr0.echotime.forge.api;

import io.github.kxyr0.echotime.api.schedule.TimeScheduleEntry;
import io.github.kxyr0.echotime.api.schedule.TimeScheduleUpdate;
import java.util.List;

public interface EchoTimeScheduleService {
    boolean replace(TimeScheduleUpdate update);

    List<TimeScheduleEntry> entries();
}
