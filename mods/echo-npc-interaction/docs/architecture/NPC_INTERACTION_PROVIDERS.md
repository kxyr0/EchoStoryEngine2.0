# NPC Interaction Providers

Provider возвращает support decision:

- `UNSUPPORTED`
- `SUPPORTED(priority, specificity)`
- `INVALID(reason)`

Selection детерминирован: выбирается максимальная specificity. Если две
поддержки имеют одинаковую максимальную specificity, результат:
`NPC_INTERACTION_PROVIDER_CONFLICT`.

Built-in provider:

- `echo_npc_interaction:vanilla_block_use`

Seat providers подключаются adapter-ами после аудита конкретного chair API.
# NI2 provider model

Providers больше не означают "один Java provider на один мебельный мод".

Normal path:

- `echo_npc_interaction:world_use`;
- generic consequence classifier;
- generic ride handoff;
- Echo seat anchor fallback.

Отдельный Java-адаптер под Handcrafted, Paladin, Refurbished или другой furniture mod не является обязательным normal path. Специализированный adapter допустим только после отдельного аудита, если generic consequence transfer не может безопасно подтвердить результат.
