# Architecture

Echo Dialogue NPC Adapter is a server-side Forge adapter.

```text
Echo Dialogue D6 request
  -> echo_dialogue_npc_adapter:npc_presentation
  -> Echo NPC public NpcService.query(ActorId)
  -> ResolvedSpeakerPresentation
```

Production packages:

- `io.github.kxyr0.echodialoguenpcadapter` - Forge entrypoint;
- `diagnostic` - bounded Russian warnings;
- `identity` - exact `SpeakerId` to `ActorId` conversion;
- `npc` - public Echo NPC service lookup wrapper;
- `presentation` - D6 provider factory, session and presentation policy.

The adapter owns no durable state. It keeps only bounded in-memory warning keys.

## Claim policy

The provider claims a speaker only when Echo NPC proves that the canonical actor
exists or when the actor exists but cannot currently produce a safe presentation.
Unknown speakers and unavailable NPC service are not claimed.
