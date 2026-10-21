# Hot reload

Datapack resources `data/<namespace>/echo_magic/spells/*.es` и внешние content packs компилируются через один parser/binder pipeline. При ошибке catalog swap не выполняется.

Следующий cast получает новую revision; активный cast и payload projectile остаются pinned на старой.

