# Effect Contributions

Core aggregation: `EffectContributionComposer`.

Reducer rules:

- movement speed: multiply;
- FOV and roll: sum;
- shake: max absolute amplitude;
- tint and vignette opacity: sum.

Cleanup означает отсутствие contribution после terminal instance.
