# Editor Camera

Камера создается client-only как `Marker`, ставится через `Minecraft#setCameraEntity` and never moves the player body.
`AnimatorCameraStateSnapshot` stores the original camera entity, perspective and HUD flag, then restores them on close.
