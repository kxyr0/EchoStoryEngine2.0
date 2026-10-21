# VIS1 Architecture

```text
EchoScript parser -> Visibility binder -> immutable catalog
                                        -> per-player arm + pinned revision
actual Render Camera -> projection/frustum broad phase -> bounded samples/rays
                     -> hysteresis transition -> protected report
                     -> same-frame NPC concealment lease
server validator -> accepted lifecycle event -> public integrations
```

`visibility-contracts` is immutable and platform-neutral. `visibility-core`
owns math, sampling, state, arms, report validation and lease state. The Forge
API exposes service/query/catalog/diagnostic/debug/concealment boundaries. The
Forge runtime alone imports Minecraft and Echo NPC's public APIs. The script
adapter invokes the real EchoScript parser and binder exactly once per source.

There are no per-frame world, NPC, Trigger, or resource scans. Armed watches are
the only evaluated set; an NPC resolves through its numeric client entity mapping
and stable ActorId, while a box has immutable bounds.
