from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class ExportKey:
    tick: int
    position: tuple[float, float, float]
    rotation: tuple[float, float, float]
    scale: tuple[float, float, float]
    interpolation: str = "LINEAR"


@dataclass(frozen=True)
class ExportTrack:
    bone: str
    keys: tuple[ExportKey, ...]


@dataclass(frozen=True)
class ExportSegment:
    name: str
    start_tick: int
    end_tick: int
    role: str


@dataclass(frozen=True)
class ExportPoint:
    name: str
    tick: int
    segment: str | None = None
    kind: str = "POINT"


@dataclass(frozen=True)
class ExportEvent:
    name: str
    tick: int
    payload: str = ""


@dataclass(frozen=True)
class RootMotion:
    mode: str = "DISABLED"
    bone: str = "root"


@dataclass(frozen=True)
class ExportAnimationModel:
    resource_id: str
    rig_id: str
    duration_ticks: int
    animation_type: str
    channel: str
    bone_mask: str
    source_state: str
    active_state: str
    target_state: str
    return_state: str
    segments: tuple[ExportSegment, ...] = ()
    points: tuple[ExportPoint, ...] = ()
    events: tuple[ExportEvent, ...] = ()
    tracks: tuple[ExportTrack, ...] = ()
    root_motion: RootMotion = field(default_factory=RootMotion)
    authored_speed: float = 0.0
    blend_in_ticks: int = 3
    blend_out_ticks: int = 3
    fallback_blend_ticks: int = 3
    hold_last_frame: bool = False
    loop_seam: str = "SMOOTH"

    @property
    def local_id(self) -> str:
        return self.resource_id.split(":", 1)[-1]

    @property
    def clip_id(self) -> str:
        return self.resource_id

    def segment(self, role: str) -> ExportSegment | None:
        return next((item for item in self.segments if item.role == role), None)


@dataclass(frozen=True)
class ValidationIssue:
    severity: str
    code: str
    message: str
    bone: str = ""


@dataclass(frozen=True)
class ExportResult:
    clip_path: str
    animation_path: str | None
    warnings: tuple[ValidationIssue, ...] = ()
