# Diagnostics

Adapter warning codes:

- `EDNA-NPC-SERVICE-001` - service lookup threw an exception;
- `EDNA-NPC-SERVICE-002` - Echo NPC service unavailable;
- `EDNA-NPC-SERVICE-003` - duplicate Echo NPC service providers;
- `EDNA-NPC-QUERY-001` - `NpcService.query` threw an exception.

Ordinary missing actors and non-NPC speakers do not log diagnostics.

Provider diagnostics returned to Echo Dialogue are Russian and bounded by the
D6 `SpeakerPresentationResolution` contract.
