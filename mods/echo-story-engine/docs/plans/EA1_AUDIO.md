# EA1 — Sound authoring boundary

Scope firewall: audio can exist independently and mentions Minecraft, so all execution is in
the separate Echo Audio mod. Only frontend AST lowering belongs in story-echoscript-adapter.

Changed files: settings.gradle, adapter build.gradle, LinearMethodBinder, intrinsic/audio,
audio compiler tests, version and architecture/progress/changelog docs.

Plan: reuse shared binder → emit portable external action payload → use Forge extension
outside Story → verify parser/binder and architecture checks → build/deploy the production JAR.

Recovery policy: incomplete audio actions fail once as interrupted; completed durable steps
are not replayed. Runtime guide: D:/JDHSeason/docs/runtime/AUDIO_DSL.md.
