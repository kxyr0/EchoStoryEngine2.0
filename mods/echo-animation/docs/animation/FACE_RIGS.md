# Face Rigs

Canonical suffix: `.face-rig.es`.

Canonical root: `data/<namespace>/echo_animation/face_rigs/`.

## High-Level Syntax

Face rig resources can use the concise face DSL:

```csharp
namespace JDH.Animation.Chapter1.Family;

face JohnFather("jdh:john_father")
{
    Eyes
    {
        Sclera Left;
        Sclera Right;
        Pupil Left;
        Pupil Right;
    }

    Blink "eye_closure";
}
```

The compiler lowers this to the same `FaceRigDefinition` model as the legacy
`FaceRig(...)` intrinsic syntax.

## Common Scenario

Only write values that differ from the conventions:

```csharp
Pupil Left
{
    Position(-0.09, 0, 0.01);
}
```

## Tuned Scenario

Use full property names when a layer needs explicit atlas or placement tuning:

```csharp
Eyelid Upper
{
    TextureArea(0, 0, 8, 2);
    Position(0, 0.065, 0.01);
}
```

## Escape Hatch

Rare non-standard elements can override generated values:

```csharp
Pupil Left
{
    Bone("left_eye_root");
    Texture("damaged_pupil");
    Role("DECORATION");
    Size(2, 2);
    Order(50);
    Position(-0.09, 0, 0.03);
}
```

## Automatic Values

Every `face` creates `face_root`.

Default texture root for `face JohnFather("jdh:john_father")`:

```text
jdh:echo_animation/faces/john_father
```

Default layer texture paths append the generated texture name, for example
`pupil_left` becomes:

```text
jdh:echo_animation/faces/john_father/pupil_left
```

Default bones:

```text
left_eye_root -> face_root
left_sclera -> left_eye_root
left_pupil -> left_sclera
right_eye_root -> face_root
right_sclera -> right_eye_root
right_pupil -> right_sclera
upper_eyelids -> face_root
upper_eyelid_left -> upper_eyelids
upper_eyelid_right -> upper_eyelids
lower_eyelids -> face_root
lower_eyelid_left -> lower_eyelids
lower_eyelid_right -> lower_eyelids
brows_left -> face_root
brows_right -> face_root
```

Default sizes:

```text
Sclera: 2x2
Pupil: 1x2
Brow: 2x1
Eyelid: 8x2 split into left/right 4x2 layers
Glasses: 8x8
```

Default draw orders:

```text
Sclera Left: 10
Sclera Right: 11
Pupil Left: 15
Pupil Right: 16
Eyelid Upper: 20
Eyelid Lower: 21
Brow Left: 30
Brow Right: 31
Glasses: 40
```

`Position(x, y, z)` uses direct face-plane units. `Pivot(x, y)`,
`Travel(upper, lower)`, and `Gaze(maxX, maxY)` use authoring milli-units:
`1000` means `1.0`. Blink timing can be authored as either
`Timing(periodTicks, closeTicks, holdTicks, openTicks)` or separate
`PeriodTicks(...)`, `CloseTicks(...)`, `HoldTicks(...)`, `OpenTicks(...)`
calls. Lower `PeriodTicks` blinks more often; lower close/open ticks blink
faster.

## Legacy Syntax

The low-level `class ... : FaceRigResource` and `return FaceRig(...)` format is
still accepted for compatibility. Treat it as a generated or legacy format for
cases where all low-level fields must be authored manually.
