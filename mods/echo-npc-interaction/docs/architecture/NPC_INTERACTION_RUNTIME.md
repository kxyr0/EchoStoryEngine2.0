# NPC Interaction Runtime

Runtime владеет lifecycle команды:

```text
EchoScript
-> script adapter
-> NpcInteractionService.start
-> target inspector
-> provider selector
-> approach resolver
-> execution port
-> alignment
-> provider execute
-> verification
-> receipt
-> script resume
```

Server thread не блокируется. Runtime хранит active records по `NpcId`, не
разрешает второй active interaction для того же NPC и отдаёт typed failure
`NPC_INTERACTION_ALREADY_ACTIVE`.

Forge runtime не импортируется contracts/core слоями. Core остаётся plain Java.
# NI2 runtime stages

Runtime разделяет стадии:

```text
APPROACH
USE
CONSEQUENCE_CLASSIFICATION
CONSEQUENCE_TRANSFER
```

`NO_APPROACH` завершается до `USE`; по нему нельзя диагностировать seat/provider failure. После `USE` результат определяется только по typed consequences и receipt v2 evidence.
