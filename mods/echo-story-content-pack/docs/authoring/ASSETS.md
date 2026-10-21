# Asset conventions

## NPC skin

`NpcSkinId` `jdh:john_father` ищется Echo NPC как:

`assets/jdh/textures/echo_npc/skins/john_father.png`

Требования:

- PNG;
- Minecraft skin 64x64;
- lowercase path;
- без spaces;
- без absolute paths, URLs или внешних файлов.

## Dialogue portrait

`Portrait("jdh:john_father")` ищется Echo Dialogue как:

`assets/jdh/textures/echo_dialogue/portraits/john_father.png`

Рекомендуется square PNG 128x128 или 256x256. Build validation принимает 16..512 px по каждой стороне.

## Static and dynamic SkinHead

`SkinHead("jdh:john_father")` ищется Echo Dialogue как:

`assets/jdh/textures/echo_dialogue/skins/john_father.png`

Это optional static authored fallback.

N4 dynamic NPC presentation возвращает тот же neutral `SKIN_HEAD`, но для
`NpcSkinId("jdh:john_father")` клиент также пробует NPC-owned path:

`assets/jdh/textures/echo_npc/skins/john_father.png`

Обычно лучше:

- держать full body skin в Echo NPC path;
- дать Dialogue отдельный authored portrait;
- получать dialogue head из N4 dynamic provider без PNG-дубликата в
  `echo_dialogue/skins`.

## Source art

`.psd`, `.xcf`, `.kra`, `.aseprite`, Blender files и временные exports не входят в `src/main/resources`. Держите их вне production JAR, например под `art-source/`, когда source art реально появится.

Font files не распространяются через content pack.

## Echo Animation face layers

Current live authoring path:

`<gameDir>/story/content-pack/jdh/assets/jdh/textures/echo_animation/faces/`

Those external assets are registered as `CLIENT_RESOURCES` by Echo Story Engine.
After changing PNGs, reload client resources with `F3+T`. Keep copies in
`src/main/resources/assets/...` only as packaged seed/fallback content.

Face-layer PNG для A0 лежат в `src/main/resources/assets/jdh/textures/echo_animation/faces/`.
Они являются обычными content-pack assets и попадают в final content JAR.

Текущий production slice:

- `assets/jdh/textures/echo_animation/faces/john_father/sclera_left.png`
- `assets/jdh/textures/echo_animation/faces/john_father/sclera_right.png`
- `assets/jdh/textures/echo_animation/faces/john_father/pupil_left.png`
- `assets/jdh/textures/echo_animation/faces/john_father/pupil_right.png`
- `assets/jdh/textures/echo_animation/faces/john_father/upper_eyelids.png`
- `assets/jdh/textures/echo_animation/faces/john_father/lower_eyelids.png`
- `assets/jdh/textures/echo_animation/faces/john_father/brow_left.png`
- `assets/jdh/textures/echo_animation/faces/john_father/brow_right.png`
- `assets/jdh/textures/echo_animation/faces/john_father/glasses.png`
- `assets/jdh/textures/echo_animation/faces/player/sclera_left.png`
- `assets/jdh/textures/echo_animation/faces/player/sclera_right.png`
- `assets/jdh/textures/echo_animation/faces/player/pupil_left.png`
- `assets/jdh/textures/echo_animation/faces/player/pupil_right.png`
- `assets/jdh/textures/echo_animation/faces/player/upper_eyelids.png`
- `assets/jdh/textures/echo_animation/faces/player/lower_eyelids.png`
- `assets/jdh/textures/echo_animation/faces/player/brow_left.png`
- `assets/jdh/textures/echo_animation/faces/player/brow_right.png`

Каждый PNG должен быть привязан в:

`src/main/resources/data/jdh/echo_animation/face_rigs/chapter_1/family/john_father.face-rig.es`

Важно: фрагмент ниже описывает текущую authored-настройку production rig и не требует
миграции файла. Для новых canonical face rigs используйте отдельные bones
`left_sclera`/`right_sclera` и делайте `left_pupil`/`right_pupil` их прямыми
детьми. Echo Animation валидирует такую canonical-схему, а текущие пары
`pupil_left`/`sclera_left` и `pupil_right`/`sclera_right` клипятся renderer-ом
без изменения `.face-rig.es` координат. Если в rig остаётся `pupil_left` или
`pupil_right`, matching `sclera_left` или `sclera_right` тоже должен быть
authored layer; иначе validation отклонит ресурс.

Рабочий шаблон настройки:

```text
Layer("brow_left", bone: "brows_left",
        texture: "jdh:echo_animation/faces/john_father/brow_left",
        role: "BROW", order: 50, x: 0, y: 0, w: 2, h: 1,
        placeX: "-0.08", placeY: "-0.09", placeZ: "0.66")
Layer("sclera_left", bone: "face_root",
        texture: "jdh:echo_animation/faces/john_father/sclera_left",
        role: "DECORATION", order: 20, x: 0, y: 0, w: 2, h: 2,
        placeX: "-0.08", placeY: 0, placeZ: "0.60")
Layer("pupil_left", bone: "face_root",
        texture: "jdh:echo_animation/faces/john_father/pupil_left",
        role: "PUPIL", order: 30, x: 0, y: 0, w: 1, h: 2,
        clipX: 0, clipY: 0, clipW: 2, clipH: 2,
        placeX: "-0.08", placeY: 0, placeZ: "0.62")
```

`x/y/w/h` выбирают пиксели из PNG. `placeX/placeY/placeZ` двигают слой на лице. `placeZ: "0.63"` задает ту же
глубину, что integer `placeZ: 630`; quoted decimals также позволяют отрицательные значения вроде
`placeX: "-0.07"`.

Echo Animation client renderer находит texture ID `jdh:echo_animation/faces/john_father/brow_left` как client asset
`assets/jdh/textures/echo_animation/faces/john_father/brow_left.png`. Если вы поменяли PNG или `Layer(...)`, пересоберите
content JAR, переустановите его и перезапустите клиент. Automatic hot reload
не перезагружает client textures.

Glasses остаются отдельным layer, чтобы authoring editor мог скрывать их во время проверки eyelids. Neutral NPC skin экспортируется authoring tool в `assets/jdh/textures/echo_npc/skins/`; старый body PNG может оставаться faceless до export.

Legacy JSON5 и исходники миграции не кладутся в `src/main/resources`.
