# Pivots

Every `Transform3` contains `translation`, `rotationZDegrees`, `scale` and explicit `pivot`.

Pivot changes rotation result without changing bone identity or parent. A pose delta in A0 does not change pivot; pivot editing belongs to authoring and must save back into FaceRig.

