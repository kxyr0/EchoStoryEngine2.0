# Restart behavior

N4 owns no persistence and writes no world data.

Echo Dialogue persists:

- `DialogueSessionId`;
- current revision;
- current speaker descriptor source values;
- current line/choice state.

Echo NPC persists:

- durable `ActorId`;
- actor desired state;
- actor display name source;
- optional `NpcSkinId`;
- materialized/dormant reconstruction state.

After restart, Echo Dialogue asks providers again for the current presentation.
N4 converts the same stored `SpeakerId` canonical value to `ActorId` and queries
the current server-scoped Echo NPC service.

N4 does not use Minecraft entity UUID, entity id or an old service handle. If
the adapter JAR is removed, Echo Dialogue recovery still has authored fallback
and does not require N4 state.

Same-line hot refresh is not implemented. A presentation is resolved when Echo
Dialogue creates or refreshes a LINE/CHOICE presentation.

## Headless restart acceptance

`runN4AcceptanceRestartChoiceServer` proves the server-side CHOICE restart path
with the same persisted world:

- first run starts `jdh:content_pack_smoke` as `jdh:n4_restart_choice`;
- Dialogue session `story-action/jdh:n4_restart_choice/play_greeting` reaches
  ACTIVE `revision=2`;
- the server stops at `ESCPC-ACCEPTANCE PAUSE_CHOICE`;
- second run restores the same session as detached/active Dialogue state;
- the acceptance harness waits for ACTIVE `revision=2` before posting
  `choiceId=accept`;
- Dialogue advances to `revision=3`, then completes with
  `outcome=jdh:content_pack_accepted`;
- N4 resolves `jdh:content_pack_father` to display `Отец` and
  `SKIN_HEAD jdh:content_pack_father` in both server lifecycles.

This is headless evidence. Client overlay pixels after restart still require a
real client check.
