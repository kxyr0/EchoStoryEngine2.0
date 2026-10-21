# Cutscene Player Actor Adapter

`echo-cutscene-player-actor-adapter` зависит только от public API Echo Cutscene и Echo Player Actor.

На `CutsceneLifecycleEvent(PLAYING, ACCEPTED)` adapter берёт `CINEMATIC_CONTROL` lease. На `COMPLETED`, `STOPPED` или `FAILED` он отпускает lease. При logout выполняется emergency release.

Adapter не управляет камерой и не импортирует cutscene implementation.
