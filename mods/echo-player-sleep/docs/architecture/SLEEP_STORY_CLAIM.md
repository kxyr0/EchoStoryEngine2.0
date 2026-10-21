# Sleep Story Claim

После vanilla confirmation session переходит в `WAITING_FOR_STORY_CLAIM`.

Успешный route:

```text
Confirmed event
-> exact Trigger subscription
-> Trigger execution
-> Story execution id
-> PlayerSleepStoryClaim
-> STORY_CLAIMED
-> Sleep.CloseEyes
```

`PlayerSleepStoryClaimService` гарантирует one-session-one-claim. Arm потребляется только после успешного claim.
Если claim не создан, `CloseEyes` отклоняется с `PLAYER_SLEEP_STORY_CLAIM_REQUIRED`.
