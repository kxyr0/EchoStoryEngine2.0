# Trigger Visibility Adapter Map

`forge.story` owns the public Story-extension bridge for `echo_visibility:*`
actions. It resolves only public player/watch state, keeps waits keyed by Story
`ActionExecutionId`, and never performs camera classification.

```text
accepted VisibilityLifecycleEvent
  -> exact VisibilitySubscriptionIndex lookup
  -> persisted VisibilityEventId claim
  -> TriggerEventIngress + TriggerService.start
  -> VisibilityStoryContextBridge.install
  -> EchoStoryExecutionBridge.start
  -> concealment monitor
       NPC absent -> COMMITTED
       ten-tick deadline -> REJECTED
```

The index is rebuilt only when the Trigger catalog revision changes. A stable
render frame performs no Trigger lookup because the Visibility runtime emits no
unchanged-state lifecycle report. The adapter never scans world entities; its
completion check uses the Echo NPC public `NpcNavigationBodyLookupEvent` for one
stable `ActorId`.

Inherited context includes player, watch and generation, visibility event,
target kind and identity/bounds, transition frames and reason, the exact active
camera snapshot summary, target revision, concealment lease, Trigger execution,
subscription, and Story execution.
