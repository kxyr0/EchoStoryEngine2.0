# Синхронизация профиля

Сервер является authority. Клиент получает immutable snapshot.

Full snapshot отправляется:

- при login;
- при reconnect;
- при tracking start;
- после серверной profile mutation;
- после явной resync.

Delta может отправляться после mutation, но revision остаётся обязательной. Клиент игнорирует stale packet, если incoming profile/repository revision старее локальной.

Клиентский mirror не становится источником истины. Он нужен только для отображения имени, Spark и base appearance.
