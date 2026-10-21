# NPC Form Runtime

Runtime-поток:

```text
.form.es
-> EchoScriptNpcFormCompiler
-> NpcDefinitionCatalog.forms
-> ActorDesiredState.activeForm*
-> ForgeActorMaterializer
-> EchoNpcEntity synched form state
-> client renderer
```

`Npc.SetForm` идет через save-before-materialization: сначала durable write,
затем применение к entity на server thread. Если запись не принята, видимая
форма не меняется.

`EchoNpcEntity` хранит synchronized width, height, eye height, global scale,
skin id и компактный form state. Renderer применяет global scale и per-part
transforms к humanoid model. Collision и eye height остаются server-authority.

Sleeping/rest attachment блокирует `SetForm`, чтобы не получить несовместимую
pose на кровати.
