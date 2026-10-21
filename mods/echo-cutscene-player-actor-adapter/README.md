# Echo Cutscene Player Actor Adapter

Adapter связывает Echo Cutscene с Echo Player Actor через public Forge API.

Он не управляет камерой, не перемещает body и не импортирует cutscene implementation. При старте playback adapter берёт `CINEMATIC_CONTROL` lease на настоящее тело игрока, а при завершении или logout освобождает control.
