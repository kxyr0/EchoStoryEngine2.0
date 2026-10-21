package io.github.kxyr0.echotime.core.mutation;

import io.github.kxyr0.echotime.api.mutation.TimeAddRequest;
import io.github.kxyr0.echotime.api.mutation.TimeCrossingPolicy;
import io.github.kxyr0.echotime.api.mutation.TimeMutationResult;
import io.github.kxyr0.echotime.api.mutation.TimeMutationStatus;
import io.github.kxyr0.echotime.api.mutation.TimeSetRequest;
import io.github.kxyr0.echotime.api.schedule.TimeCrossingReason;
import io.github.kxyr0.echotime.api.value.EchoDay;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import io.github.kxyr0.echotime.core.clock.ClockTransition;
import io.github.kxyr0.echotime.core.clock.EchoClock;
import io.github.kxyr0.echotime.core.schedule.ScheduleCrossingResult;
import io.github.kxyr0.echotime.core.schedule.TimeOccurrenceSchedule;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TimeMutationExecutor {
    private final EchoClock clock;
    private final TimeOccurrenceSchedule schedule;
    private final TimeMutationLedger ledger;
    private final int maximumCrossedEvents;

    public TimeMutationExecutor(EchoClock clock, TimeOccurrenceSchedule schedule,
                                TimeMutationLedger ledger, int maximumCrossedEvents) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.schedule = Objects.requireNonNull(schedule, "schedule");
        this.ledger = Objects.requireNonNull(ledger, "ledger");
        if (maximumCrossedEvents <= 0) throw new IllegalArgumentException("maximumCrossedEvents must be positive.");
        this.maximumCrossedEvents = maximumCrossedEvents;
    }

    public MutationExecution set(TimeSetRequest request, long serverTick) {
        Optional<MutationExecution> previous = previous(request.mutationId(), request.signature(),
                request.crossingPolicy());
        if (previous.isPresent()) return previous.orElseThrow();
        EchoInstant old = clock.instant();
        try {
            EchoDay day = request.day().orElseGet(old::day);
            long target = EchoInstant.at(day, request.time(), old.revision()).absoluteMicroseconds();
            return apply(request.mutationId(), request.signature(), request.source(), old, target,
                    request.crossingPolicy(), serverTick);
        } catch (ArithmeticException | IllegalArgumentException exception) {
            return failed(request.mutationId(), request.signature(), request.source(), old,
                    request.crossingPolicy(), TimeMutationStatus.OVERFLOW, exception.getMessage());
        }
    }

    public MutationExecution add(TimeAddRequest request, long serverTick) {
        Optional<MutationExecution> previous = previous(request.mutationId(), request.signature(),
                request.crossingPolicy());
        if (previous.isPresent()) return previous.orElseThrow();
        EchoInstant old = clock.instant();
        try {
            long target = Math.addExact(old.absoluteMicroseconds(), request.duration().microseconds());
            return apply(request.mutationId(), request.signature(), request.source(), old, target,
                    request.crossingPolicy(), serverTick);
        } catch (ArithmeticException exception) {
            return failed(request.mutationId(), request.signature(), request.source(), old,
                    request.crossingPolicy(), TimeMutationStatus.OVERFLOW, exception.getMessage());
        }
    }

    private MutationExecution apply(
            io.github.kxyr0.echotime.api.identity.TimeMutationId id,
            String signature,
            io.github.kxyr0.echotime.api.mutation.TimeRequestSource source,
            EchoInstant old,
            long target,
            TimeCrossingPolicy policy,
            long serverTick) {
        EchoInstant predicted = new EchoInstant(target, old.revision().next());
        ScheduleCrossingResult crossing = policy == TimeCrossingPolicy.FIRE_CROSSED
                ? schedule.crossed(old, predicted, TimeCrossingReason.SCRIPT_TIME_JUMP, maximumCrossedEvents)
                : new ScheduleCrossingResult(List.of(), false);
        if (crossing.limitExceeded()) {
            return failed(id, signature, source, old, policy, TimeMutationStatus.CROSSING_LIMIT_EXCEEDED,
                    "ECHO_TIME_CROSSED_EVENT_LIMIT");
        }
        ClockTransition transition = clock.setAbsolute(target, serverTick);
        TimeMutationResult result = new TimeMutationResult(id, TimeMutationStatus.APPLIED,
                transition.previous(), transition.current(), policy, "Time mutation applied.");
        ledger.record(new TimeMutationReceipt(id, signature, source, result));
        return new MutationExecution(result, crossing.occurrences());
    }

    private Optional<MutationExecution> previous(
            io.github.kxyr0.echotime.api.identity.TimeMutationId id,
            String signature,
            TimeCrossingPolicy policy) {
        return ledger.find(id).map(receipt -> {
            TimeMutationResult stored = receipt.result();
            TimeMutationStatus status = receipt.signature().equals(signature)
                    ? TimeMutationStatus.DUPLICATE : TimeMutationStatus.IDEMPOTENCY_CONFLICT;
            String message = status == TimeMutationStatus.DUPLICATE
                    ? "ECHO_TIME_MUTATION_DUPLICATE" : "ECHO_TIME_MUTATION_IDEMPOTENCY_CONFLICT";
            return new MutationExecution(new TimeMutationResult(id, status, stored.previous(), stored.current(),
                    policy, message), List.of());
        });
    }

    private MutationExecution failed(
            io.github.kxyr0.echotime.api.identity.TimeMutationId id,
            String signature,
            io.github.kxyr0.echotime.api.mutation.TimeRequestSource source,
            EchoInstant instant,
            TimeCrossingPolicy policy,
            TimeMutationStatus status,
            String message) {
        TimeMutationResult result = new TimeMutationResult(id, status, instant, instant, policy,
                message == null ? status.name() : message);
        ledger.record(new TimeMutationReceipt(id, signature, source, result));
        return new MutationExecution(result, List.of());
    }
}
