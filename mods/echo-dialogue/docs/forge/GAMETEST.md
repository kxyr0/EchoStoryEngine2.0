# GameTest

Текущие GameTests:

- `echoDialogueCommandsExist`: проверяет регистрацию `/echo_dialogue` command root и D1 literals.
- `dialogueResourceCompilesInForgeRuntime`: компилирует `DialogueResource` в GameTest runtime и проверяет русский текст.
- `controlledAudienceSessionLifecycle`: запускает session для controlled audience, проверяет first line, server advance,
  duplicate/stale/wrong-session rejection, ordered line progression и `COMPLETED` final close.
- `choiceSessionBranchesToExplicitOutcome`: проверяет ordered choices, `SelectChoice`, branch target и explicit
  `DialogueOutcomeId`.
- `invalidCandidateRetainsCatalog`: проверяет, что semantic invalid candidate не заменяет previous known-good catalog, diagnostics содержит source subject, а fixed candidate затем применяется.
- `durableSnapshotRestoresSameLine`: записывает snapshot, создаёт fresh repository и проверяет same session/revision/current line.
- `incompatibleFingerprintBlocksRecovery`: проверяет, что changed compiled meaning блокирует recovery.
- `terminalRecordCanBeAcknowledged`: сохраняет terminal record, читает/acknowledge и проверяет release.
- `worldRootsAreIsolated`: проверяет, что разные world roots не разделяют session storage.
- `visualAliasesCompile`: проверяет `Portrait` и `SkinHead` alias binding.
- `invalidVisualRetainsCatalog`: проверяет rejection invalid visual intrinsic и known-good retention.
- `presentationsTransferVisualDescriptors`: проверяет LINE/CHOICE prompt visual descriptors и no-visual speaker.
- `changedVisualBlocksRecovery`: проверяет fingerprint block при изменении authored visual ID.
- `providerJarRegistersThroughPublicEvent`: проверяет, что отдельный provider JAR регистрируется через public Forge event.
- `dynamicLinePresentationUsesProvider`: проверяет dynamic LINE display/visual и неизменность line text/revision.
- `dynamicChoicePresentationKeepsChoices`: проверяет dynamic CHOICE prompt и неизменность choice IDs/order.
- `providerFallbackScenariosUseAuthoredPresentation`: проверяет NOT_HANDLED, unavailable, failure, exception и malformed fallback.
- `ambiguousProvidersUseAuthoredFallback`: проверяет ambiguity fallback и diagnostic с provider IDs.
- `providerSessionsCloseWithCatalog`: проверяет закрытие provider sessions вместе с catalog.

Gradle post-check читает `run/logs/latest.log`, проверяет маркеры `EDLG-GAMETEST-RUN` и извлекает discovered/executed/passed counts.

Acceptance guard:

- discovered count должен быть не меньше `19`;
- executed count должен быть не меньше `19`;
- passed count должен быть не меньше `19`;
- все required marker names должны быть в log;
- отсутствие required marker или zero-test execution ломает Gradle task.

Оставшийся предел GameTest-покрытия: GameTests не проверяют actual client texture rendering, alpha, GUI-scale, visual restart и mouse selection визуально. Dedicated server client-classloading, separate-provider packaging и final-JAR packaging проверяются отдельными smoke/artifact задачами.
