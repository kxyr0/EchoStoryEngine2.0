# EchoScript authoring

EchoScript remains syntax-only. Echo Animation owns these semantic resource types:

- `FaceRigResource`;
- `FacePoseResource`;
- `FacialAnimationResource`.

The binder reads the immutable syntax tree and returns compiled definitions. It does not execute source code, compile Java, use reflection or retain EchoScript AST inside runtime definitions.

Unsupported fields produce `EANIM-ES-UNSUPPORTED-FIELD`.

## Unified procedural body animations (0.5)

`AnimationClipResource.Build()` and `AnimationResource.Build()` are procedural `void` builders. One clip owns one
integer-tick timeline; `Segment` and `Point` are named zero-copy views over it. `Event(name, at, payload)` records
one-shot timeline metadata, while `RootMotion(Disabled|Extract|Preserve, bone)` declares root-track ownership. Typed symbols such as `Smooth`,
`Cubic`, `Start`, `Bounded` and `RejectIfRunning` are not quoted.

```csharp
public override void Build() {
    Id("jdh:run"); Rig("jdh:humanoid"); Duration(17);
    var loop = Segment("run_loop", from: 2, to: 14);
    Loop(loop, seam: Smooth);
    Point(loop, "neutral", at: End);
    Event("footstep", at: 5, payload: "left");
    RootMotion(Extract, bone: "root");
    var body = Track("body");
    Key(body, 2, Cubic).Rotation(-8, 0, 0);
    Key(body, 14).Rotation(-8, 0, 0);
}
```

Animation builders independently support `Enter`, `Loop`, `Hold`, `OneShot` and `Exit`. `Fallback(Crossfade(...))`,
`Blend`, `BlendIn`, `BlendOut`, `Mask`, `Recovery` and `Replay` compile into runtime policy. Segment durations and
point progress are resolved from the referenced clip; authors do not repeat `durationTicks`.

Legacy `return AnimationClip(...)` and `return Animation(...)` remain readable during migration and emit
`EANIM-ES-LEGACY`. Mixing procedural statements with a legacy return is rejected as `EANIM-ES-MIXED-BUILD`.

Числа в transform-like fields поддерживают две формы:

- integer thousandths: `placeZ: 630`;
- quoted decimal units: `placeZ: "0.63"`.

Для отрицательных значений используйте quoted decimals, например `placeX: "-0.07"`, потому что A0 EchoScript parser
не поддерживает bare unary minus literal.
