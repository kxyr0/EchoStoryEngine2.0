# ES2 feature unlock authoring

Translate `Feature.Unlock`, `Feature.Lock`, and `Feature.Set` in the EchoScript frontend into generic
`echo_core:set_flag` external action nodes. Runtime gameplay remains in Echo Core; Story contracts/core and
the standalone EchoScript parser remain unchanged. Existing generic Action syntax remains compatible.

Files: new `intrinsic/features/FeatureActionIntrinsics`, LinearMethodBinder dispatch, compiler tests,
authoring docs/project map/progress, and mod version 1.9.0. Verify ordered graph links, defaults, player override,
aliases, strict argument rejection, adapter architecture tests, root checks, and Forge packaging. Install the
resulting Story Engine JAR in the existing Prism instance, preserving the old JAR outside mods.
