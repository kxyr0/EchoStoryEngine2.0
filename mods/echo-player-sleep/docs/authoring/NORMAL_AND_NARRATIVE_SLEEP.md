# Normal And Narrative Sleep

Обычный сон:

```text
No Arm
-> vanilla sleep
-> no narrative event
-> no Trigger execution
-> no Story
```

Сюжетный сон:

```text
Sleep.Arm
-> vanilla confirmation
-> PlayerSleepSession
-> Confirmed event
-> exact Trigger subscription
-> Story claim
-> StoryResource
```

Клик по кровати сам по себе не является `Confirmed`. Событие публикуется только после активного arm, успешного vanilla
sleep state и сохраненной `PlayerSleepSession`.
