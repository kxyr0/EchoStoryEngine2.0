from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path


_NUMBER = r"[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?"


@dataclass(frozen=True)
class ParsedFrame:
    time: float
    interpolation: str | None
    position: tuple[float, float, float] | None = None
    rotation: tuple[float, float, float] | None = None
    scale: tuple[float, float, float] | None = None
    matrix: tuple[float, ...] | None = None


@dataclass(frozen=True)
class ParsedTrack:
    bone_name: str
    frames: tuple[ParsedFrame, ...]


@dataclass(frozen=True)
class ParsedClip:
    resource_id: str
    rig_id: str
    schema_version: int
    loop_seam: str
    tracks: tuple[ParsedTrack, ...]
    duration_ticks: int | None = None
    legacy: bool = False


@dataclass(frozen=True)
class ParsedPhase:
    name: str
    clip_id: str
    duration_ticks: int


@dataclass(frozen=True)
class ParsedAnimation:
    resource_id: str
    mode: str
    main: ParsedPhase
    enter: ParsedPhase | None = None
    exit: ParsedPhase | None = None
    legacy: bool = False

    @property
    def phases(self) -> tuple[ParsedPhase, ...]:
        result = []
        if self.enter is not None:
            result.append(self.enter)
        result.append(self.main)
        if self.exit is not None:
            result.append(self.exit)
        return tuple(result)


def _float_tuple(payload: str) -> tuple[float, ...]:
    return tuple(float(value) for value in re.findall(_NUMBER, payload))


def _string_or_number_tuple(payload: str) -> tuple[float, ...]:
    quoted = re.findall(r'"([^"\\]*(?:\\.[^"\\]*)*)"', payload)
    if quoted:
        return tuple(float(value) for value in quoted)
    return _float_tuple(payload)


def _balanced_block(text: str, start_index: int, open_char: str, close_char: str) -> tuple[str, int]:
    if start_index >= len(text) or text[start_index] != open_char:
        raise ValueError(f"Expected {open_char!r} at offset {start_index}")
    depth = 0
    in_string = False
    escaped = False
    for index in range(start_index, len(text)):
        char = text[index]
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
            continue
        if char == '"':
            in_string = True
            continue
        if char == open_char:
            depth += 1
        elif char == close_char:
            depth -= 1
            if depth == 0:
                return text[start_index + 1:index], index + 1
    raise ValueError(f"Unclosed {open_char}{close_char} block")


def _call_payload(text: str, call_name: str, start: int = 0) -> tuple[str, int] | None:
    match = re.search(rf"\b{re.escape(call_name)}\s*\(", text[start:])
    if match is None:
        return None
    open_index = start + match.end() - 1
    payload, end = _balanced_block(text, open_index, "(", ")")
    return payload, end


def _named_string(payload: str, name: str) -> str | None:
    match = re.search(rf"\b{re.escape(name)}\s*:\s*\"([^\"]+)\"", payload)
    return match.group(1) if match else None


def _named_int(payload: str, name: str) -> int | None:
    match = re.search(rf"\b{re.escape(name)}\s*:\s*(-?\d+)", payload)
    return int(match.group(1)) if match else None


def _parse_v2_frame(expr: str) -> ParsedFrame:
    frame_call = _call_payload(expr, "Frame")
    if frame_call is None:
        raise ValueError(f"Invalid Frame expression: {expr[:100]}")
    payload, _ = frame_call
    time_match = re.match(r'\s*\"([^\"]+)\"', payload)
    if time_match is None:
        raise ValueError(f"Frame time is missing: {expr[:100]}")
    time = float(time_match.group(1))
    interpolation = _named_string(payload, "interpolation")

    def vector(name: str) -> tuple[float, float, float] | None:
        call = _call_payload(expr, name)
        if call is None:
            return None
        values = _float_tuple(call[0])
        if len(values) != 3:
            raise ValueError(f"{name} requires 3 values, got {len(values)}")
        return values  # type: ignore[return-value]

    matrix_call = _call_payload(expr, "Matrix")
    matrix = None
    if matrix_call is not None:
        matrix = _string_or_number_tuple(matrix_call[0])
        if len(matrix) != 16:
            raise ValueError(f"Matrix requires 16 values, got {len(matrix)}")

    return ParsedFrame(
        time=time,
        interpolation=interpolation,
        position=vector("Position"),
        rotation=vector("Rotation"),
        scale=vector("Scale"),
        matrix=matrix,
    )


def _split_v2_frames(frames_payload: str) -> tuple[ParsedFrame, ...]:
    starts = [match.start() for match in re.finditer(r"\bFrame\s*\(", frames_payload)]
    result = []
    for index, start in enumerate(starts):
        end = starts[index + 1] if index + 1 < len(starts) else len(frames_payload)
        expr = frames_payload[start:end].strip().rstrip(",").strip()
        result.append(_parse_v2_frame(expr))
    return tuple(result)


def _parse_v2_clip(text: str) -> ParsedClip:
    clip_call = _call_payload(text, "AnimationClip")
    if clip_call is None:
        raise ValueError("AnimationClip(...) not found")
    payload, _ = clip_call
    id_match = re.match(r'\s*\"([^\"]+)\"', payload)
    if id_match is None:
        raise ValueError("AnimationClip resource ID not found")
    resource_id = id_match.group(1)
    rig_id = _named_string(payload, "rig") or "jdh:humanoid"
    schema_version = _named_int(payload, "schemaVersion") or 2
    loop_seam = (_named_string(payload, "loopSeam") or "HOLD").upper()

    tracks_marker = re.search(r"\btracks\s*:\s*\[", payload)
    if tracks_marker is None:
        return ParsedClip(resource_id, rig_id, schema_version, loop_seam, ())
    open_index = tracks_marker.end() - 1
    tracks_payload, _ = _balanced_block(payload, open_index, "[", "]")

    tracks = []
    cursor = 0
    while True:
        match = re.search(r'\bTrack\s*\(\s*\"([^\"]+)\"\s*,\s*frames\s*:\s*\[', tracks_payload[cursor:])
        if match is None:
            break
        bone_name = match.group(1)
        absolute_open = cursor + match.end() - 1
        frames_payload, array_end = _balanced_block(tracks_payload, absolute_open, "[", "]")
        tracks.append(ParsedTrack(bone_name, _split_v2_frames(frames_payload)))
        cursor = array_end
    return ParsedClip(resource_id, rig_id, schema_version, loop_seam, tuple(tracks))


def _parse_legacy_clip(text: str) -> ParsedClip:
    id_match = re.search(r'\bId\s*\(\s*\"([^\"]+)\"\s*\)', text)
    rig_match = re.search(r'\bRig\s*\(\s*\"([^\"]+)\"\s*\)', text)
    duration_match = re.search(r"\bDuration\s*\(\s*(\d+)\s*\)", text)
    if id_match is None:
        raise ValueError("Legacy clip Id(...) not found")
    duration = int(duration_match.group(1)) if duration_match else None
    var_to_bone = {
        match.group(1): match.group(2)
        for match in re.finditer(r'\bvar\s+(\w+)\s*=\s*Track\s*\(\s*\"([^\"]+)\"\s*\)\s*;', text)
    }
    frames_by_bone: dict[str, list[ParsedFrame]] = {bone: [] for bone in var_to_bone.values()}
    key_pattern = re.compile(r"\bKey\s*\(\s*(\w+)\s*,\s*(\d+)\s*,\s*(\w+)\s*\)([^;]*);", re.DOTALL)
    for match in key_pattern.finditer(text):
        variable, tick_text, interpolation, tail = match.groups()
        bone_name = var_to_bone.get(variable)
        if bone_name is None:
            continue

        def vector(name: str) -> tuple[float, float, float] | None:
            vector_match = re.search(rf"\.{name}\s*\(([^)]*)\)", tail)
            if vector_match is None:
                return None
            values = _float_tuple(vector_match.group(1))
            if len(values) != 3:
                raise ValueError(f"Legacy {name} requires 3 values")
            return values  # type: ignore[return-value]

        frames_by_bone[bone_name].append(
            ParsedFrame(
                time=float(tick_text),
                interpolation=interpolation.upper(),
                position=vector("Position"),
                rotation=vector("Rotation"),
                scale=vector("Scale"),
            )
        )
    tracks = tuple(
        ParsedTrack(bone, tuple(sorted(frames, key=lambda frame: frame.time)))
        for bone, frames in frames_by_bone.items()
        if frames
    )
    return ParsedClip(
        resource_id=id_match.group(1),
        rig_id=rig_match.group(1) if rig_match else "jdh:humanoid",
        schema_version=1,
        loop_seam="SMOOTH" if re.search(r"\bLoop\s*\(", text) else "HOLD",
        tracks=tracks,
        duration_ticks=duration,
        legacy=True,
    )


def parse_clip_text(text: str) -> ParsedClip:
    if re.search(r"\breturn\s+AnimationClip\s*\(", text):
        return _parse_v2_clip(text)
    return _parse_legacy_clip(text)


def parse_clip_file(path: str | Path) -> ParsedClip:
    return parse_clip_text(Path(path).read_text(encoding="utf-8-sig"))


def _parse_v2_animation(text: str) -> ParsedAnimation:
    call = _call_payload(text, "Animation")
    if call is None:
        raise ValueError("Animation(...) not found")
    payload, _ = call
    id_match = re.match(r'\s*\"([^\"]+)\"', payload)
    if id_match is None:
        raise ValueError("Animation resource ID not found")
    resource_id = id_match.group(1)

    main_match = re.search(r"\bmain\s*:\s*Main\s*\(", payload)
    if main_match is None:
        raise ValueError("main: Main(...) not found")
    main_open = main_match.end() - 1
    main_payload, _ = _balanced_block(payload, main_open, "(", ")")
    main_clip = _named_string(main_payload, "clip")
    main_mode = (_named_string(main_payload, "mode") or "ONCE").upper()
    main_ticks = _named_int(main_payload, "durationTicks")
    if not main_clip or main_ticks is None:
        raise ValueError("Main clip/durationTicks not found")

    enter = None
    entry_match = re.search(r"\bEntry\s*\(", payload)
    if entry_match is not None:
        entry_payload, _ = _balanced_block(payload, entry_match.end() - 1, "(", ")")
        clip_id = _named_string(entry_payload, "clip")
        ticks = _named_int(entry_payload, "durationTicks")
        if clip_id and ticks is not None:
            enter = ParsedPhase("enter", clip_id, ticks)

    exit_phase = None
    exit_match = re.search(r"\bExit\s*\(", payload)
    if exit_match is not None:
        exit_payload, _ = _balanced_block(payload, exit_match.end() - 1, "(", ")")
        clip_id = _named_string(exit_payload, "clip")
        ticks = _named_int(exit_payload, "durationTicks")
        if clip_id and ticks is not None:
            exit_phase = ParsedPhase("exit", clip_id, ticks)

    return ParsedAnimation(
        resource_id=resource_id,
        mode=main_mode,
        main=ParsedPhase("main", main_clip, main_ticks),
        enter=enter,
        exit=exit_phase,
    )


def _legacy_segment_durations(text: str) -> dict[str, tuple[int, int]]:
    result = {}
    for match in re.finditer(
        r'\bvar\s+\w+\s*=\s*Segment\s*\(\s*\"([^\"]+)\"\s*,\s*from\s*:\s*(\d+)\s*,\s*to\s*:\s*(\d+)\s*\)',
        text,
    ):
        result[match.group(1)] = (int(match.group(2)), int(match.group(3)))
    return result


def _parse_legacy_animation(text: str) -> ParsedAnimation:
    id_match = re.search(r'\bId\s*\(\s*\"([^\"]+)\"\s*\)', text)
    clip_match = re.search(r'\bvar\s+\w+\s*=\s*Clip\s*\(\s*\"([^\"]+)\"\s*\)', text)
    if id_match is None or clip_match is None:
        raise ValueError("Legacy animation Id/Clip not found")
    resource_id = id_match.group(1)
    clip_id = clip_match.group(1)
    mode = "LOOP_UNTIL_STOP" if re.search(r"\bLoop\s*\(", text) else "ONCE"
    # Old AnimationResource does not necessarily carry a total clip duration.
    # The clip file itself normally has Duration(...), so 0 means "resolve from clip".
    return ParsedAnimation(resource_id, mode, ParsedPhase("main", clip_id, 0), legacy=True)


def parse_animation_text(text: str) -> ParsedAnimation:
    if re.search(r"\breturn\s+Animation\s*\(", text):
        return _parse_v2_animation(text)
    return _parse_legacy_animation(text)


def parse_animation_file(path: str | Path) -> ParsedAnimation:
    return parse_animation_text(Path(path).read_text(encoding="utf-8-sig"))


def resource_local_name(resource_id: str) -> str:
    return resource_id.split(":", 1)[-1].strip().replace("/", "_")
