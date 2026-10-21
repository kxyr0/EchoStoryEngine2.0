# Hierarchy

A0 hierarchy editing targets `Bone(...)` entries inside `.face-rig.es`.

Rules:

- duplicate `BoneId` is rejected by production validation;
- missing parent is rejected;
- cycle reparent is rejected;
- reparent must preserve world transform before saving.

In-game visual hierarchy display is pending manual acceptance.

