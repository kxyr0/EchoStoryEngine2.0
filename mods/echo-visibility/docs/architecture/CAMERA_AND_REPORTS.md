# Camera and perception reports

The camera snapshot is captured at Forge `AFTER_CUTOUT_BLOCKS`, after Minecraft
has applied active camera ownership, POV, yaw/pitch and dynamic projection, and
before entity rendering. It includes camera/player bases, framebuffer dimensions
and the exact column-major projection matrix.

The classifier transforms all AABB corners into clip space. Fully outside and
behind-camera results finish in broad phase. Intersecting targets remain visible
until every in-viewport sample ray is conclusively blocked. Failed or uncertain
rays are conservative and never create a false `OCCLUDED` result.

Reports are transition-only and carry generation, pinned revision, connection
generation, monotonic sequence, client tick/render frame, transition, camera,
sample fingerprint and optional lease. Server validation rejects stale streams,
revision/generation mismatches, invalid bases/transitions and illegal leases.
