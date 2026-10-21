# Npc.WakeUp

`Npc.WakeUp(npcId)` снимает durable rest attachment и освобождает occupied state кровати через тот же provider, который укладывал NPC.

Пример:

```echoscript
Npc.WakeUp(Sasha);
```

Wake не вызывается автоматически из Dialogue. Автор сцены явно решает, когда NPC должен проснуться. Если NPC уже не спит, команда возвращает идемпотентный `ALREADY_AWAKE`; если provider не может выдать безопасную позицию подъема, используется диагностика `NPC_WAKE_POSITION_INVALID`.

Командный аналог: `/echo_npc wake_up <npc_id>`.
