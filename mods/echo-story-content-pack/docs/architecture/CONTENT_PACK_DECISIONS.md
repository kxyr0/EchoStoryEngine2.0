# Content pack decisions

## Content-only Forge mod

Принято: один Forge low-code mod с `lowcodefml`, без production Java. Причина: C1 должен упаковать resources, а не создать еще один runtime.

## Stable namespace `jdh`

Принято: authored IDs используют `jdh:*`. Forge mod ID остается `echo_story_content_pack`. Эти identity levels намеренно разные.

## Separate resource trees

Принято: Story, Dialogue и NPC лежат в отдельных loader trees. Связь выражается IDs, а не folder nesting.

## Authored fallback visuals

Принято: Dialogue `Alias(...)` хранит fallback display/visual. D6 dynamic provider может заменить presentation runtime-only, но authored fallback остается обязательным.

## No duplicate manifest

Принято: не вводить второй manifest со списком Story/Dialogue/NPC IDs. Authority остается в `.es` files.

## Semantic validation by engines

Принято: content pack проверяет layout/assets/JAR shape. Story/Dialogue/NPC semantics подтверждают actual runtime compilers/catalogs.

## One final content JAR

Принято: distribution artifact - `echo-story-content-pack-<version>.jar`. Docs, templates, acceptance resources и source art не входят в production JAR.

