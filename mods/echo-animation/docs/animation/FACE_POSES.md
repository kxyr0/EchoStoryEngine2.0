# Face poses

Canonical suffix: `.face-pose.es`.

FacePose stores deltas only:

```text
return FacePose("jdh:john_father/neutral", rig: "jdh:john_father",
    bones: [BoneDelta("face_root")],
    controls: [ControlDelta("eye_closure", value: 0)]
);
```

A pose cannot change parent or pivot. Rig compatibility is validated against the compiled FaceRig.

