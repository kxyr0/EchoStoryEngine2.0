package io.github.kxyr0.echotime.forge.persistence;

import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.identity.TimePauseLease;
import io.github.kxyr0.echotime.api.identity.TimePauseOwner;
import io.github.kxyr0.echotime.api.identity.TimeWaitId;
import io.github.kxyr0.echotime.api.mutation.TimeCrossingPolicy;
import io.github.kxyr0.echotime.api.mutation.TimeMutationResult;
import io.github.kxyr0.echotime.api.mutation.TimeMutationStatus;
import io.github.kxyr0.echotime.api.mutation.TimeRequestSource;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import io.github.kxyr0.echotime.api.value.TimeRate;
import io.github.kxyr0.echotime.api.value.TimeRevision;
import io.github.kxyr0.echotime.core.mutation.TimeMutationReceipt;
import io.github.kxyr0.echotime.core.state.EchoTimeState;
import io.github.kxyr0.echotime.core.state.TimeWait;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class EchoTimeStateCodec {
    public CompoundTag encode(EchoTimeState state) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema", state.schemaVersion());
        instant(tag, "instant", state.instant());
        tag.putLong("rateNumerator", state.rate().gameMicroseconds());
        tag.putLong("rateDenominator", state.rate().simulationTicks());
        tag.putLong("remainder", state.fixedPointRemainder());
        tag.putBoolean("running", state.configuredRunning());
        tag.putLong("configRevision", state.configRevision());
        tag.put("pauseLeases", pauses(state.pauseLeases()));
        tag.put("mutationReceipts", receipts(state.mutationReceipts()));
        tag.put("waits", waits(state.waits()));
        return tag;
    }

    public EchoTimeState decode(CompoundTag tag) {
        TimeRate rate = new TimeRate(tag.getLong("rateNumerator"), tag.getLong("rateDenominator"));
        return new EchoTimeState(tag.getInt("schema"), instant(tag, "instant"), rate,
                tag.getLong("remainder"), tag.getBoolean("running"), tag.getLong("configRevision"),
                pauses(tag.getList("pauseLeases", Tag.TAG_COMPOUND)),
                receipts(tag.getList("mutationReceipts", Tag.TAG_COMPOUND)),
                waits(tag.getList("waits", Tag.TAG_COMPOUND)));
    }

    private ListTag pauses(List<TimePauseLease> values) {
        ListTag list = new ListTag();
        values.forEach(value -> {
            CompoundTag tag = new CompoundTag();
            tag.putString("owner", value.owner().value());
            tag.putString("mutation", value.acquiredBy().value());
            list.add(tag);
        });
        return list;
    }

    private List<TimePauseLease> pauses(ListTag list) {
        List<TimePauseLease> values = new ArrayList<>();
        for (Tag raw : list) {
            CompoundTag tag = (CompoundTag) raw;
            values.add(new TimePauseLease(new TimePauseOwner(tag.getString("owner")),
                    new TimeMutationId(tag.getString("mutation"))));
        }
        return values;
    }

    private ListTag receipts(List<TimeMutationReceipt> values) {
        ListTag list = new ListTag();
        values.forEach(value -> list.add(receipt(value)));
        return list;
    }

    private CompoundTag receipt(TimeMutationReceipt value) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", value.mutationId().value());
        tag.putString("signature", value.signature());
        source(tag, value.source());
        TimeMutationResult result = value.result();
        tag.putString("status", result.status().name());
        tag.putString("policy", result.crossingPolicy().name());
        tag.putString("message", result.message());
        instant(tag, "previous", result.previous());
        instant(tag, "current", result.current());
        return tag;
    }

    private List<TimeMutationReceipt> receipts(ListTag list) {
        List<TimeMutationReceipt> values = new ArrayList<>();
        for (Tag raw : list) {
            CompoundTag tag = (CompoundTag) raw;
            TimeMutationId id = new TimeMutationId(tag.getString("id"));
            TimeMutationResult result = new TimeMutationResult(id,
                    TimeMutationStatus.valueOf(tag.getString("status")), instant(tag, "previous"),
                    instant(tag, "current"), TimeCrossingPolicy.valueOf(tag.getString("policy")),
                    tag.getString("message"));
            values.add(new TimeMutationReceipt(id, tag.getString("signature"), source(tag), result));
        }
        return values;
    }

    private ListTag waits(List<TimeWait> values) {
        ListTag list = new ListTag();
        values.forEach(value -> {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", value.waitId().value());
            tag.putLong("time", value.requestedTime().microseconds());
            tag.putLong("target", value.targetAbsoluteMicroseconds());
            list.add(tag);
        });
        return list;
    }

    private List<TimeWait> waits(ListTag list) {
        List<TimeWait> values = new ArrayList<>();
        for (Tag raw : list) {
            CompoundTag tag = (CompoundTag) raw;
            values.add(new TimeWait(new TimeWaitId(tag.getString("id")),
                    new TimeOfDay(tag.getLong("time")), tag.getLong("target")));
        }
        return values;
    }

    private void instant(CompoundTag parent, String key, EchoInstant value) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("absolute", value.absoluteMicroseconds());
        tag.putLong("revision", value.revision().value());
        parent.put(key, tag);
    }

    private EchoInstant instant(CompoundTag parent, String key) {
        CompoundTag tag = parent.getCompound(key);
        return new EchoInstant(tag.getLong("absolute"), new TimeRevision(tag.getLong("revision")));
    }

    private void source(CompoundTag tag, TimeRequestSource value) {
        tag.putString("sourceModule", value.module());
        tag.putString("sourceSystem", value.system());
        tag.putString("sourceImmediate", value.immediateOwner());
        tag.putString("sourceRoot", value.rootOwner());
        value.sourceResource().ifPresent(resource -> tag.putString("sourceResource", resource));
        value.sourceLine().ifPresent(line -> tag.putInt("sourceLine", line));
        tag.putString("sourceReason", value.reason());
    }

    private TimeRequestSource source(CompoundTag tag) {
        Optional<String> resource = tag.contains("sourceResource")
                ? Optional.of(tag.getString("sourceResource")) : Optional.empty();
        Optional<Integer> line = tag.contains("sourceLine")
                ? Optional.of(tag.getInt("sourceLine")) : Optional.empty();
        return new TimeRequestSource(tag.getString("sourceModule"), tag.getString("sourceSystem"),
                tag.getString("sourceImmediate"), tag.getString("sourceRoot"), resource, line,
                tag.getString("sourceReason"));
    }
}
