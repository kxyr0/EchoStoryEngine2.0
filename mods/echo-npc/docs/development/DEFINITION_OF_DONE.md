# Definition of Done

N2 считается завершенным на automated уровне только если выполнены все пункты.
Manual acceptance после EULA and visual client checks фиксируется отдельно и не
заявляется без фактического прогона.

## Code

- Production dependency graph соответствует N2.
- Добавлен ровно один новый module: `npc-persistence`.
- `ActorId` остается logical identity and not Minecraft entity UUID.
- Actor Save Schema V1 хранит durable desired state, а не Entity/Server objects.
- Spawn, teleport and despawn persist desired state before materialization.
- Startup recovery validates exact definition fingerprint.
- Missing/incompatible/corrupt actors become blocked and do not stop unrelated
  recovery.
- Unloaded chunks make actors dormant without forced chunk loading.
- Entity reconciliation uses `ActorId + EntityBindingId` and preserves unrelated
  nearby Echo NPC entities.
- Public API exposes operation read models without Path/File/Gson/Minecraft
  implementation details.
- Commands and diagnostics are Russian where user-facing.

## Automated Verification

- `projects`;
- `dependencies`;
- `test`;
- `:npc-contracts:test :npc-core:test :npc-persistence:test`;
- `architectureTest`;
- `sourceShapeCheck`;
- `checkWhitespace`;
- `check`;
- `build`;
- `:npc-forge-1.20.1:runGameTestServer`;
- `:npc-forge-1.20.1:verifyProductionJarContents`;
- publication metadata under `build/integration-maven`;
- forbidden production scans;
- `git diff --check` либо documented невозможность из-за отсутствия git repo.

## Manual Acceptance

- normal dedicated server после ручного EULA;
- isolated final-JAR server;
- real client;
- isolated final-JAR client;
- manual spawn, duplicate, conflict, teleport, despawn;
- authored skin и missing-skin fallback;
- reload-pinning;
- restart recovery for spawn/teleport/despawn;
- chunk unload/reload behavior;
- world-switch behavior.
