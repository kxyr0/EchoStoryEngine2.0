# Player Actor в Cutscene

Cutscene управляет камерой и cinematic input policy. Player Actor управляет только настоящим body.

`echo-cutscene-player-actor-adapter` берёт `CINEMATIC_CONTROL` lease на `CutsceneLifecycleEvent` со статусом `PLAYING` и отпускает lease при terminal lifecycle.

Detached camera не вращается и не двигается из-за `Player.Rotate`, `Player.LookAt` или `Player.Move`.
