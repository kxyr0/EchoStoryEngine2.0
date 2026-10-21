# NPC Forms

Форма - authored snapshot тела и внешности одного durable NPC.

Идентичность разделена:

- `ActorId`: durable runtime identity, например `jdh:sasha`;
- `NpcDefinitionId`: authored NPC definition;
- `NpcFormId`: authored body/appearance form, например `jdh:sasha/child`;
- `EntityBindingId`: internal binding к текущей Minecraft entity.

`NpcDefinition` может иметь `defaultFormId`. При spawn runtime пинит эту форму в
`ActorDesiredState.activeForm*`; повторные загрузки мира восстанавливают тот же
snapshot без подмены личности.

Каталог форм компилируется атомарно вместе с NPC definitions. Если `defaultForm`
указывает на отсутствующую форму, reload не устанавливает частично сломанный
catalog и пишет diagnostic `NPC_FORM_NOT_FOUND`.
