# Echo Save Migration

Каждый save содержит typed `EchoSaveManifest` с глобальным `SaveSchemaVersion`, migration revision, content
fingerprint и module persistence versions. Migration resources разбираются общим EchoScript parser и допускают только
pure операции над `SaveSnapshot`; world, entity, camera, time и gameplay API в migration context отсутствуют.

Запись выполняется как транзакция: immutable source → backup → candidate → structural/content validation → durable
write → read-back и SHA-256 verify → atomic replace. Journal проходит состояния `PLANNED`, `BACKED_UP`, `APPLYING`,
`CANDIDATE_WRITTEN`, `VERIFIED`, `COMMITTED`. Recovery либо завершает проверенный commit, либо оставляет source без
изменений. Stable `MigrationId` и receipts не допускают повторного применения.
