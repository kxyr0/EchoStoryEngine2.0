# Echo Compatibility: WorldEdit

Forge 1.20.1 compatibility guard for WorldEdit 7.2.15 in large modpacks.

WorldEdit normally precomputes every state combination for every registered block during
`ServerStartedEvent`. A modded block with a very large Cartesian product can keep the integrated
server at the 100% world-loading screen indefinitely. This mod replaces that unbounded prewarm with
a bounded equivalent.

- Blocks with at most 4096 estimated states are registered normally.
- Larger blocks are skipped and named in `latest.log`.
- WorldEdit remains available for ordinary blocks and commands.
- A skipped pathological block may not be usable through WorldEdit.

Advanced override: add `-Decho.worldedit.maxStatesPerBlock=<number>` to the instance Java arguments.
