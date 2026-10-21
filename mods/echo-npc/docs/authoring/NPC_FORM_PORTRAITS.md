# Form Portraits

`portrait` в форме задает portrait resource для диалогов и UI. Это не `ActorId`
и не skin id. Один NPC может иметь разные portraits для детской и взрослой
формы:

```echoscript
portrait: "jdh:sasha/child"
```

Интеграции должны читать active form snapshot через `NpcService.getForm` или
`NpcActorView.activeFormId`, а не угадывать portrait по имени NPC.

Если portrait отсутствует в downstream системе, dialogue adapter должен
использовать свой fallback portrait и диагностировать missing resource, не
пересоздавая NPC и не меняя форму.
