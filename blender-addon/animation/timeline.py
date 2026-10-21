from __future__ import annotations

import math

from .markers import HOLD, STATE_END, STATE_START, marker_frame, sync_from_action
from .model import ExportEvent, ExportPoint, ExportSegment
from .naming import sanitize_name, semantic_names


def frame_range(action, metadata) -> tuple[int, int]:
    start, end = int(metadata.frame_start), int(metadata.frame_end)
    if end <= start and action.frame_range[1] > action.frame_range[0]:
        start, end = math.floor(action.frame_range[0]), math.ceil(action.frame_range[1])
    return start, end


def duration_ticks(start: int, end: int, fps: float, tick_rate: float, override: int = 0) -> int:
    if override > 0:
        return override
    return max(1, round(((end - start) / fps) * tick_rate))


def frame_to_tick(frame: int, start: int, end: int, duration: int) -> int:
    return round(((frame - start) / (end - start)) * duration)


def tick_to_frame(tick: int, start: int, end: int, duration: int) -> float:
    return start + (tick / duration) * (end - start)


def boundary_frames(action, metadata) -> dict[str, int]:
    start, end = frame_range(action, metadata)
    state_start = marker_frame(action, STATE_START, start)
    state_end = marker_frame(action, STATE_END, end)
    hold = marker_frame(action, HOLD, state_start)
    return {"start": start, "state_start": state_start, "state_end": state_end, "hold": hold, "end": end}


def build_segments(action, metadata, duration: int) -> tuple[ExportSegment, ...]:
    sync_from_action(action)
    frames = boundary_frames(action, metadata)
    start, end = frames["start"], frames["end"]
    names = semantic_names(metadata.source_state, metadata.active_state, metadata.target_state)
    names["enter"] = metadata.enter_segment_name or names["enter"]
    names["loop"] = metadata.loop_segment_name or names["loop"]
    names["exit"] = metadata.exit_segment_name or names["exit"]
    result = []

    def add(role: str, first: int, last: int):
        first_tick = frame_to_tick(first, start, end, duration)
        last_tick = frame_to_tick(last, start, end, duration)
        result.append(ExportSegment(names[role], first_tick, last_tick, role))

    kind = metadata.animation_type
    if kind == "LOOP":
        add("loop", frames["state_start"], frames["state_end"])
    elif kind in {"STATEFUL_LOOP", "LOCOMOTION"}:
        if metadata.enter_enabled and start < frames["state_start"]:
            add("enter", start, frames["state_start"])
        if metadata.loop_enabled:
            add("loop", frames["state_start"], frames["state_end"])
        if metadata.exit_enabled and frames["state_end"] < end:
            add("exit", frames["state_end"], end)
    elif kind == "STATEFUL_HOLD":
        hold = frames["hold"]
        if metadata.enter_enabled and start < hold:
            add("enter", start, hold)
        if metadata.exit_enabled and hold < end:
            add("exit", hold, end)
    elif kind == "ADVANCED":
        if metadata.enter_enabled and start < frames["state_start"]:
            add("enter", start, frames["state_start"])
        if metadata.loop_enabled:
            add("loop", frames["state_start"], frames["state_end"])
        if metadata.exit_enabled and frames["state_end"] < end:
            add("exit", frames["state_end"], end)
    return tuple(result)


def _owning_segment(tick: int, segments: tuple[ExportSegment, ...], preferred: str = "") -> str | None:
    if preferred and any(item.name == preferred and item.start_tick <= tick <= item.end_tick for item in segments):
        return preferred
    matches = [item for item in segments if item.start_tick <= tick <= item.end_tick]
    matches.sort(key=lambda item: item.role != "loop")
    return matches[0].name if matches else None


def build_points_events(action, metadata, duration: int, segments: tuple[ExportSegment, ...]):
    sync_from_action(action)
    start, end = frame_range(action, metadata)
    points, events = [], []
    if metadata.animation_type == "STATEFUL_HOLD":
        frame = boundary_frames(action, metadata)["hold"]
        tick = frame_to_tick(frame, start, end, duration)
        points.append(ExportPoint(sanitize_name(metadata.hold_point_name, "hold_pose"), tick, _owning_segment(tick, segments), "HOLD"))
    for item in metadata.markers:
        tick = frame_to_tick(item.frame, start, end, duration)
        if item.kind == "EVENT":
            events.append(ExportEvent(sanitize_name(item.name, "event"), tick, item.payload))
        else:
            kind = item.kind
            fallback = "left_contact" if kind == "LEFT_FOOT" else "right_contact" if kind == "RIGHT_FOOT" else "point"
            points.append(ExportPoint(sanitize_name(item.name, fallback), tick, _owning_segment(tick, segments, item.segment), kind))
    return tuple(points), tuple(events)


def required_frames(action, metadata) -> set[int]:
    values = set(boundary_frames(action, metadata).values())
    values.update(int(item.frame) for item in metadata.markers)
    return values
