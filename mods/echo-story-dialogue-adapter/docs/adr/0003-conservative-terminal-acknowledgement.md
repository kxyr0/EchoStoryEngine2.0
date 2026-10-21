# ADR 0003: Conservative Terminal Acknowledgement

Статус: принято.

Решение: Story completion отправляется только после terminal `DialogueSessionView` из public Echo Dialogue API.

Причины:

- `start` подтверждает принятие Dialogue request, но не завершение действия;
- save-before-presentation остаётся ответственностью Echo Dialogue;
- post-completion crash window покрывается durable terminal retention Dialogue;
- adapter не сообщает Story success до завершения linear dialogue.

Следствие: adapter не использует polling every tick и не пишет своё terminal хранилище.
