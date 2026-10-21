# Fatigue Stages

The first version contains three fatigue stages.

| Stage | Reserve condition | Movement multiplier | Nausea |
| --- | --- | ---: | ---: |
| `RESTED` | `reserve > 50%` | `1.0` | `0.0` |
| `TIRED` | `25% < reserve <= 50%` | `0.95` | `0.0` |
| `EXHAUSTED` | `reserve <= 25%` | `0.85` | `0.35` |

Stage rules must cover the whole `0..1` range without gaps or ambiguous overlaps.

On conflict, diagnostics report `PLAYER_VITALS_FATIGUE_STAGE_AMBIGUOUS`.
