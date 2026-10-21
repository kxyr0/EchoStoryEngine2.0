# Provider Resolution D6

For every LINE or speakerful CHOICE presentation, Forge host executes one deterministic policy:

1. Build authored `DialoguePresentation` from compiled dialogue and session state.
2. Build `SpeakerPresentationRequest`.
3. Invoke all active provider sessions sorted by `SpeakerPresentationProviderId`.
4. Collect claiming results.
5. Build the network presentation from either the validated dynamic result or authored fallback.

Claiming results:

- `RESOLVED`;
- `TEMPORARILY_UNAVAILABLE`;
- `FAILURE`.

`NOT_HANDLED` means the provider does not own the speaker and does not claim the request.

## Outcomes

- No claims: authored fallback.
- Exactly one `RESOLVED`: use dynamic display name and dynamic optional visual.
- Exactly one unavailable/failure/malformed/exception: authored fallback plus bounded diagnostic.
- Two or more claims: authored fallback plus deterministic ambiguity diagnostic with sorted provider IDs.

Registration order is never a winner policy. D6 has no numeric priorities and no partial merge. A provider that wants text-only dynamic presentation returns a full `ResolvedSpeakerPresentation` with no visual.

## CHOICE

Only explicit speaker context in the authoritative CHOICE presentation is resolved. A CHOICE without prompt does not query providers and does not reuse a previous portrait.
