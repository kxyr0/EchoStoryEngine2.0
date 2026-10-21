package io.github.kxyr0.echotime.forge.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.identity.TimePauseLease;
import io.github.kxyr0.echotime.api.identity.TimePauseOwner;
import io.github.kxyr0.echotime.api.identity.TimeWaitId;
import io.github.kxyr0.echotime.api.mutation.TimeCrossingPolicy;
import io.github.kxyr0.echotime.api.mutation.TimeMutationResult;
import io.github.kxyr0.echotime.api.mutation.TimeMutationStatus;
import io.github.kxyr0.echotime.api.mutation.TimeRequestSource;
import io.github.kxyr0.echotime.api.value.EchoDay;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import io.github.kxyr0.echotime.api.value.TimeRate;
import io.github.kxyr0.echotime.api.value.TimeRevision;
import io.github.kxyr0.echotime.core.mutation.TimeMutationReceipt;
import io.github.kxyr0.echotime.core.state.EchoTimeState;
import io.github.kxyr0.echotime.core.state.TimeWait;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class EchoTimeStateCodecTest {
    @Test void completeDurableStateRoundTripsThroughNbt() {
        EchoInstant old = EchoInstant.at(EchoDay.FIRST, TimeOfDay.parse("09:40"), TimeRevision.INITIAL);
        EchoInstant current = EchoInstant.at(EchoDay.FIRST, TimeOfDay.parse("09:50"), new TimeRevision(2));
        TimeMutationId mutationId = new TimeMutationId("story:add");
        TimeRequestSource source = TimeRequestSource.command("story:one", "test");
        TimeMutationResult result = new TimeMutationResult(mutationId, TimeMutationStatus.APPLIED, old, current,
                TimeCrossingPolicy.FIRE_CROSSED, "applied");
        EchoTimeState state = new EchoTimeState(1, current, new TimeRate(3_000_000, 1), 0, true, 4,
                List.of(new TimePauseLease(new TimePauseOwner("story:one"), new TimeMutationId("pause:one"))),
                List.of(new TimeMutationReceipt(mutationId, "ADD|600000000|FIRE_CROSSED", source, result)),
                List.of(new TimeWait(new TimeWaitId("wait:one"), TimeOfDay.parse("10:00"),
                        TimeOfDay.MICROS_PER_HOUR * 10)));

        EchoTimeState decoded = new EchoTimeStateCodec().decode(
                new EchoTimeStateCodec().encode(state));
        assertEquals(state, decoded);
    }
}

