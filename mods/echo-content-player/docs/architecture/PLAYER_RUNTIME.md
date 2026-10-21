# Runtime профиля игрока

Runtime делится по владельцам thread state.

Server thread владеет:

- repository;
- lifecycle;
- mutations;
- revision ordering;
- save-before-publish;
- snapshot synchronization.

Watcher thread должен владеть только file events, fingerprints, candidate compilation и immutable reload plans. Он не мутирует runtime.

Network thread только декодирует packet и enqueue apply. Client thread владеет mirror snapshot и resource reload scheduling. Render thread читает immutable `PlayerAppearanceSnapshot`.

P1 не хранит квесты, dialogue state, trigger state, inventory, wardrobe layers, RPG stats или UI settings.
