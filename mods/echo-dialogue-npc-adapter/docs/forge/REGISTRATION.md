# Forge registration

`EchoDialogueNpcAdapterMod` subscribes to:

```java
EchoDialogueSpeakerPresentationRegistrationEvent
```

and registers:

```text
echo_dialogue_npc_adapter:npc_presentation
```

Provider descriptor:

- display name: `Echo NPC Speaker Presentation`;
- implementation version: project version;
- required API version: D6 current API from `SpeakerPresentationProviderDescriptor`.

The adapter does not use Forge lifecycle events for persistent state.
