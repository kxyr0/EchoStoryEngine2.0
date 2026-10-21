# Потоки

Story вызывает handlers на server thread. Адаптер отправляет NPC mutations в
том же call path и наблюдает terminal state во время Forge server tick.

Адаптер:

- не создает worker threads;
- не использует executors;
- не блокируется в ожидании persistence;
- не хранит ссылки на Minecraft entity;
- не вызывает completion из произвольных adapter threads.

Story completion sink thread-safe, но N3 adapter использует server tick path.
