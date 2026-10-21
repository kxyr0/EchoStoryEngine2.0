# Echo Dialogue Cutscene Adapter

Forge 1.20.1 adapter that connects Echo Dialogue external waits to Echo Cutscene public services.

Dialogue scripts can use `Cutscene.Start(...)`, `Cutscene.Play(...)`, `Cutscene.Wait(...)`, and `Cutscene.Stop(...)`
after the Echo Dialogue EchoScript adapter has compiled those calls into `echo_cutscene:*` external waits.

`Cutscene.Start(id, holdLastFrame: true)` leaves the camera on the final frame until `Cutscene.Stop(id)` runs.
`hold: true` is accepted as a short alias.
