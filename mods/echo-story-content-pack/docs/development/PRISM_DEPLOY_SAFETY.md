# Prism deploy safety

`minecraft/story` in the live Prism instance is user-owned working content. Treat it as source, not as a disposable staging directory.

Never delete or recreate these live folders during deploy:

- `minecraft/story`
- `minecraft/story/content-pack/jdh`
- `minecraft/story/content-pack/jdh/data`
- `minecraft/story/content-pack/jdh/assets`
- `minecraft/story/configs`
- `minecraft/story/item-pack`

Use the safe merge-only deploy script:

```powershell
.\tools\Deploy-PrismStoryContentPack.ps1 -WhatIf
.\tools\Deploy-PrismStoryContentPack.ps1
```

The script:

- refuses to run when `minecraft/story` has uncommitted git changes, unless `-AllowDirtyStoryGit` is passed deliberately;
- copies only `src/main/resources/data`, `src/main/resources/assets`, and `pack.mcmeta` into `story/content-pack/jdh`;
- never uses delete operations;
- backs up overwritten target files into `build/prism-deploy-backups/<timestamp>`;
- supports `-IncludeStoryConfigs` when `src/main/resources/story/configs` must be merged into `minecraft/story/configs`.

Manual deploy is allowed only for specific known files after:

```powershell
git -C "<minecraft>/story" status --short
```

Do not use mirror copy, clean sync, `Remove-Item -Recurse`, `robocopy /MIR`, or any equivalent deletion-based command against the live Prism story tree.
