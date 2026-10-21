from __future__ import annotations

import uuid


STATE_START = "ECHO_STATE_START"
STATE_END = "ECHO_STATE_END"
HOLD = "ECHO_HOLD"
ITEM_MARKERS = (
    "echo_item_attachments:attach_left_hand",
    "echo_item_attachments:attach_right_hand",
    "echo_item_attachments:release",
    "echo_item_attachments:detach",
)


def _prefix(name: str) -> str:
    return name.split(" · ", 1)[0]


def find(action, marker_id: str):
    return next((marker for marker in action.pose_markers if _prefix(marker.name) == marker_id), None)


def set_marker(action, marker_id: str, frame: int, label: str):
    marker = find(action, marker_id)
    if marker is None:
        marker = action.pose_markers.new(f"{marker_id} · {label}")
    else:
        marker.name = f"{marker_id} · {label}"
    marker.frame = int(frame)
    return marker


def remove_marker(action, marker_id: str) -> bool:
    marker = find(action, marker_id)
    if marker is None:
        return False
    action.pose_markers.remove(marker)
    return True


def marker_frame(action, marker_id: str, fallback: int | None = None) -> int | None:
    marker = find(action, marker_id)
    return int(marker.frame) if marker else fallback


def add_semantic(metadata, action, kind: str, name: str, frame: int, payload: str = "", segment: str = ""):
    if kind in {"LEFT_FOOT", "RIGHT_FOOT"}:
        count = sum(item.kind == kind for item in metadata.markers)
        if count:
            name = f"{name}_{count + 1}"
    token = uuid.uuid4().hex[:10].upper()
    prefix = "EVENT" if kind == "EVENT" else "POINT"
    marker_id = f"ECHO_{prefix}_{token}"
    label = f"Event: {name}" if kind == "EVENT" else f"Point: {name}"
    if kind == "LEFT_FOOT":
        marker_id, label = f"ECHO_LEFT_FOOT_{token}", "Left Foot"
    elif kind == "RIGHT_FOOT":
        marker_id, label = f"ECHO_RIGHT_FOOT_{token}", "Right Foot"
    set_marker(action, marker_id, frame, label)
    item = metadata.markers.add()
    item.marker_id, item.kind, item.name = marker_id, kind, name
    item.frame, item.payload, item.segment = int(frame), payload, segment
    return item


def add_item_marker(metadata, action, name: str, frame: int):
    if name not in ITEM_MARKERS:
        raise ValueError(f"Unsupported item marker: {name}")
    for index in range(len(metadata.markers) - 1, -1, -1):
        if metadata.markers[index].kind == "EVENT" and metadata.markers[index].name == name:
            remove_semantic(metadata, action, index)
    return add_semantic(metadata, action, "EVENT", name, frame)


def sync_from_action(action) -> None:
    metadata = action.echo_animation
    known = {item.marker_id: item for item in metadata.markers}
    for marker in action.pose_markers:
        marker_id = _prefix(marker.name)
        item = known.get(marker_id)
        if item is not None:
            item.frame = int(marker.frame)


def sorted_echo_markers(action):
    return sorted((marker for marker in action.pose_markers if marker.name.startswith("ECHO_")), key=lambda marker: (marker.frame, marker.name))


def remove_semantic(metadata, action, index: int) -> bool:
    if index < 0 or index >= len(metadata.markers):
        return False
    marker_id = metadata.markers[index].marker_id
    remove_marker(action, marker_id)
    metadata.markers.remove(index)
    metadata.marker_index = min(metadata.marker_index, max(0, len(metadata.markers) - 1))
    return True


def remove_duplicates(action) -> int:
    seen, removed = set(), 0
    for marker in list(action.pose_markers):
        marker_id = _prefix(marker.name)
        if not marker_id.startswith("ECHO_"):
            continue
        if marker_id in seen:
            action.pose_markers.remove(marker)
            removed += 1
        else:
            seen.add(marker_id)
    return removed
