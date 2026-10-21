# Player Control Lease

`PlayerControlLease` задаёт exclusive owner для тела игрока.

В обычной игре active lease отсутствует, и human control считается свободным. Во время катсцены adapter создаёт `CINEMATIC_CONTROL` lease с root owner `CutscenePlaybackId`.

Команда принимается, если owner совпадает с root owner или входит в authorised lineage. Конфликтующий lease возвращает `PLAYER_ACTOR_CONTROL_ALREADY_LEASED`.
