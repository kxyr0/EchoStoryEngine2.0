# NPC Interaction Recovery

Recovery rules:

- восстановить command id и execution id;
- проверить receipt;
- проверить target;
- восстановить NavigationMoveId;
- не создавать duplicate interaction;
- не нажимать кнопку второй раз;
- не создавать duplicate seat attachment.

Если receipt отсутствует, но world witness уже изменился, runtime должен перейти
в `UNCERTAIN` и diagnostic `NPC_INTERACTION_OUTCOME_UNCERTAIN`, а не повторять
опасный toggle автоматически.

