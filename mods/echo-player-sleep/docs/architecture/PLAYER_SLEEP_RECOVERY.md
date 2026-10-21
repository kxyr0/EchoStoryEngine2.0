# Player Sleep Recovery

Core содержит `PlayerSleepRecoveryDecision`.

Forge recovery command переводит active session в `RECOVERING` и публикует lifecycle event. Полная durable recovery должна заменить in-memory repository на persistent storage и применить return policy до показа мира игроку.

Recovery blackout считается начатым только вместе с safe return. Один чёрный overlay без возврата игрока не считается восстановлением.
