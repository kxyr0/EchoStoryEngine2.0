# Terminal Results

Completion mapping:

- `COMPLETED` -> `ExternalActionResultCategory.SUCCESS`
- `CANCELLED` -> `ExternalActionResultCategory.CANCELLATION`
- `ACTIVE`, `FAILED`, `BLOCKED` -> `ExternalActionResultCategory.FAILURE`

Adapter отправляет `ActionCompletion` один раз на observed terminal callback или сразу при recovery terminal view.

Echo Dialogue отвечает за durable terminal retention. Adapter не пишет отдельное хранилище terminal states и не создаёт
polling loop.
