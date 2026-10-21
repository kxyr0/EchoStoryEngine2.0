# Time Skip Transition

Смена детской формы на взрослую форму должна происходить за закрытым переходом:
черный экран, loading card, fade или scripted camera cut.

Рекомендуемый порядок:

1. Закрыть кадр.
2. Остановить навигацию и активные scene actions.
3. `Npc.WakeUp`, если NPC спит.
4. `Npc.SetForm(jdh:sasha, jdh:sasha/adult)`.
5. `Npc.PlaceAt(...)` в позицию, которая подходит adult dimensions.
6. Обновить dialogue portrait и animation graph из active form snapshot.
7. Открыть кадр.

Не создавай `jdh:sasha_child` и `jdh:sasha_adult` как разные NPC. Это ломает
диалоги, сюжетные ссылки и durable actor state.
