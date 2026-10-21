# NPC rest providers

`NpcRestProvider` описывает public boundary для объектов отдыха. Реестр provider не раскрывает internal repository и не возвращает mutable entity через core API.

Vanilla provider:

- поддерживает `minecraft:*_bed`;
- валидирует actual block id против `expectedBlock`;
- нормализует head/foot к canonical head position;
- выставляет occupied на обе половины bed;
- задает `startSleeping`, anchor и yaw;
- снимает occupied и no-ai при wake.

Будущие furniture providers должны повторять ту же модель: exact authored block, deterministic canonical position, durable attachment и явный release.
