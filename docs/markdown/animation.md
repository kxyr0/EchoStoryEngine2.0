# JDH Season - справочник по анимациям (animation.md)

Дата сборки: 2026-07-29.  
Проект: `D:\JDHSeason`.

Этот файл - единый поисковый справочник по анимациям для разработчиков сюжета. Здесь собраны телесные body-анимации, лицевые риги, кости, слои лица, лицевые facial-анимации, портретные overlay-слои, привязка NPC к профилю анимаций, вызовы из Story и Dialogue, текущий инвентарь ресурсов и практические шаблоны кода.

Главная идея: сюжетник почти всегда работает не с Java-кодом напрямую, а с EchoScript ресурсами в `mods\echo-story-content-pack\src\main\resources`. Java-код важен как источник правил: какие поля разрешены, какие ID валидны, какие кости существуют, как работают `SetAnimationFace`, `StopFaceAnimation`, `echo_animation:play_animation` и проверки валидатора.

## Быстрый минимум

Если нужно быстро добавить эмоцию в диалог:

1. У персонажа должен быть `FaceRigResource`, например `face JohnFather("jdh:john_father")`.
2. У портрета должен быть `PortraitOverlay("jdh:john_father", ...)`, если эмоция должна быть видна в интерфейсе диалога.
3. Должна существовать `FacialAnimation("jdh:john_father/angry_brows", rig: "jdh:john_father", surfaces: [World, Portrait])`.
4. В `DialogueResource` alias должен указывать на тот же visual ID: `Alias("father", "jdh:john_father", display: "Отец", visual: Portrait("jdh:john_father"));`.
5. На реплике ставится событие cue: `Line("father", "Ты поздно вернулся.").SetAnimationFace(FatherAngryBrows);`.
6. Для мягкого выхода используется `.StopFaceAnimation()`, для полного сброса состояния - `.SetAnimationFaceClear()`.

Если нужно запустить body-анимацию из Story:

1. У NPC должен быть `NpcAnimationBinding("jdh:john_father_standard_humanoid", npc: "jdh:john_father", profile: "jdh:standard_humanoid")`.
2. В profile должен быть rig `jdh:humanoid`.
3. Должна существовать высокоуровневая анимация, например `jdh:look_around_once`.
4. Story вызывает `echo_animation:play_animation` с `animation` и `target`.
5. Для `ONCE` можно ждать `await: "COMPLETED"`, для циклических/удерживаемых анимаций обычно только `await: "STARTED"`, затем отдельный `stop_animation` и `wait_animation`.

## Главные директории

```text
D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\data\jdh\echo_animation
D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\data\jdh\echo_animation\animation_rigs
D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\data\jdh\echo_animation\animation_clips
D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\data\jdh\echo_animation\animations
D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\data\jdh\echo_animation\animation_profiles
D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\data\jdh\echo_animation\face_rigs
D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\data\jdh\echo_animation\face_poses
D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\data\jdh\echo_animation\face_animations
D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\data\jdh\echo_npc_animation\bindings
D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\story\configs\jdh\echo_animation\portrait_overlays
D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\assets\jdh\textures\echo_animation
D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\assets\jdh\textures\echo_dialogue\portraits
D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\assets\jdh\textures\echo_npc\skins
D:\JDHSeason\mods\echo-story-engine\skins
```

## Runtime-пак PrismLauncher

Важно: в игре используется не только копия в репозитории из `D:\JDHSeason`. Для установленного экземпляра PrismLauncher есть внешний runtime content-pack:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\content-pack\jdh
```

И отдельная runtime-папка конфигов:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\configs\jdh
```

В коде это задано через:

```java
// mods/echo-story-engine/.../ExternalContentPaths.java
public static final String SEED_ROOT = "story/content-pack/jdh";
public static final String CONFIG_SEED_ROOT = "story/configs/jdh";

public Path packRoot() {
    return gameDirectory.resolve("story").resolve("content-pack").resolve("jdh");
}

public Path configRoot() {
    return gameDirectory.resolve("story").resolve("configs").resolve("jdh");
}
```

`content-pack\jdh` регистрируется как обязательный Forge resource/data pack:

```java
// mods/echo-story-engine/.../ExternalContentPackSource.java
Pack pack = Pack.readMetaAndCreate(
    ExternalContentPaths.PACK_ID,
    Component.literal(ExternalContentPaths.PACK_TITLE),
    true,
    id -> new PathPackResources(id, packRoot, false),
    packType,
    Pack.Position.TOP,
    PackSource.WORLD);
```

Практический вывод:

- `D:\JDHSeason\mods\echo-story-content-pack\src\main\resources` - исходная seed-копия в репозитории.
- `...\minecraft\story\content-pack\jdh` - runtime-пак, который редактируется и подхватывается в экземпляре PrismLauncher.
- `...\minecraft\story\configs\jdh` - runtime-клиентские конфиги, в том числе портретные overlay-слои.
- Seed extractor добавляет новые seed-файлы, но не перезаписывает существующие пользовательские правки.

`pack.mcmeta` в Prism pack:

```json
{
  "pack": {
    "pack_format": 15,
    "description": "Echo Story Content Pack resources"
  }
}
```

На момент сканирования Prism pack содержит:

| Тип | Количество |
|---|---:|
| `.es` | 248 |
| `.png` | 202 |
| `.json` | 7 |
| `.mcmeta` | 1 |
| `.disabled` | 1 |
| `.bak-20260726-161210` | 1 |

### Структуры папок внутри Prism-пака

В Prism pack сейчас есть два разных layout-а.

Структура datapack/resource-manager:

```text
content-pack\jdh\data\jdh\echo_animation\...
content-pack\jdh\data\jdh\echo_story\...
content-pack\jdh\data\jdh\echo_dialogue\...
content-pack\jdh\data\jdh\echo_npc\...
content-pack\jdh\data\jdh\echo_npc_animation\bindings\...
content-pack\jdh\assets\jdh\textures\...
```

Структура внешнего сканера:

```text
content-pack\jdh\echo_animation\...
content-pack\jdh\echo_npc_animation\bindings\...
content-pack\jdh\echo_wardrobe\profiles\...
content-pack\jdh\echo_wardrobe\wearables\...
content-pack\jdh\echo_wardrobe\outfits\...
```

Структура конфигов:

```text
story\configs\jdh\echo_animation\portrait_overlays\...
story\configs\jdh\echo_dialogue\client_overlay\...
```

`echo-animation` получает внешний корень как:

```java
FMLPaths.GAMEDIR.get().resolve("story").resolve("content-pack")
```

и прямой внешний сканер принимает только пути ресурсов вида:

```text
jdh/echo_animation/face_rigs/...
jdh/echo_animation/face_poses/...
jdh/echo_animation/face_animations/...
jdh/echo_animation/animations/...
jdh/echo_animation/animation_profiles/...
jdh/echo_animation/animation_rigs/...
jdh/echo_animation/animation_clips/...
```

При этом `content-pack\jdh` ещё зарегистрирован как datapack/resource pack, поэтому `data\jdh\echo_animation\...` тоже может попадать через `ResourceManager.listResources(...)`.

Практическое правило: не держать один и тот же ID ресурса одновременно в `data\jdh\...` и на верхнем уровне `echo_animation\...` / `echo_npc_animation\...`, если не хотите диагностики дублей.

### Анимационные ресурсы в Prism-паке

Лицевые риги в runtime-паке:

| ID FaceRig | Файл | Особенности |
|---|---|---|
| `jdh:john_father` | `data\jdh\echo_animation\face_rigs\chapter_1\family\john_father.face-rig.es` | глаза около Y `0`, нижние веки `0.09`, есть `Bone Glasses`, моргание `Travel(35, -35)`, период `30` |
| `jdh:sasha` | `data\jdh\echo_animation\face_rigs\chapter_1\family\sasha.face-rig.es` | шаблон лица как у друзей, глаза Y `0.06`, нижние веки `0.153`, моргание `Travel(65, -61)`, период `60` |
| `jdh:ben` | `data\jdh\echo_animation\face_rigs\chapter_1\friends\ben.face-rig.es` | шаблон как у друзей, нет `Bone Glasses` |
| `jdh:dinara` | `data\jdh\echo_animation\face_rigs\chapter_1\friends\dinara.face-rig.es` | шаблон как у друзей, нет `Bone Glasses` |
| `jdh:natan` | `data\jdh\echo_animation\face_rigs\chapter_1\friends\natan.face-rig.es` | шаблон как у друзей, нет `Bone Glasses` |
| `jdh:sofa` | `data\jdh\echo_animation\face_rigs\chapter_1\friends\sofa.face-rig.es` | шаблон как у друзей, нет `Bone Glasses` |
| `jdh:molly` | `data\jdh\echo_animation\face_rigs\chapter_1\work\molly.face-rig.es` | шаблон как у друзей, есть `Bone Glasses { Position(0, 0.06, 0.04); Size(6, 2); }` |
| `echo_content_player:player/base` | `data\jdh\echo_animation\face_rigs\chapter_1\player\player_base.face-rig.es` | риг игрока, переопределение текстур `Textures("jdh:echo_animation/faces/player")` |

Runtime-папки текстур лица:

```text
assets\jdh\textures\echo_animation\faces\ben
assets\jdh\textures\echo_animation\faces\dinara
assets\jdh\textures\echo_animation\faces\john_father
assets\jdh\textures\echo_animation\faces\molly
assets\jdh\textures\echo_animation\faces\natan
assets\jdh\textures\echo_animation\faces\player
assets\jdh\textures\echo_animation\faces\sasha
assets\jdh\textures\echo_animation\faces\sofa
```

Инвентарь ассетов слоев по папкам:

| Папка | Количество PNG | Заметки |
|---|---:|---|
| `ben` | 9 | есть `ben.png` и обычные слои лица; нет `glasses.png` |
| `dinara` | 8 | обычные слои лица |
| `john_father` | 9 | обычные слои лица и `glasses.png` |
| `molly` | 9 | обычные слои лица и `glasses.png` |
| `natan` | 8 | обычные слои лица |
| `player` | 8 | обычные слои лица |
| `sasha` | 8 | обычные слои лица |
| `sofa` | 8 | обычные слои лица |

Обычный набор ассетов слоев лица:

```text
brow_left.png
brow_right.png
lower_eyelids.png
pupil_left.png
pupil_right.png
sclera_left.png
sclera_right.png
upper_eyelids.png
```

Дополнительный ассет очков:

```text
glasses.png
```

Портретные overlay-конфиги в runtime-конфигах:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\configs\jdh\echo_animation\portrait_overlays\ben.portrait-overlay.es
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\configs\jdh\echo_animation\portrait_overlays\dinara.portrait-overlay.es
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\configs\jdh\echo_animation\portrait_overlays\john_father.portrait-overlay.es
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\configs\jdh\echo_animation\portrait_overlays\molly.portrait-overlay.es
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\configs\jdh\echo_animation\portrait_overlays\natan.portrait-overlay.es
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\configs\jdh\echo_animation\portrait_overlays\player_base.portrait-overlay.es
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\configs\jdh\echo_animation\portrait_overlays\sasha.portrait-overlay.es
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\configs\jdh\echo_animation\portrait_overlays\sofa.portrait-overlay.es
```

Также есть папка:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\configs\jdh\echo_animation copy\portrait_overlays
```

`PortraitOverlayConfigSource` сканирует все `.portrait-overlay.es` рекурсивно под `story\configs`, а не только `echo_animation\portrait_overlays`. Если в `echo_animation copy` лежит overlay с тем же `PortraitOverlay("id")`, он может перезаписать нормальный overlay в `Map<String, PortraitOverlayConfig>`. Порядок `Files.find(...)` не стоит считать авторским инструментом выбора версии. Для чистого runtime лучше не держать дублирующиеся ID overlay в `echo_animation copy`.

Runtime-позы лица:

| Файл | Сейчас объявляет | Ожидаемо по имени файла | Статус |
|---|---|---|---|
| `family\john_father_neutral.face-pose.es` | `FacePose("jdh:john_father/neutral", rig: "jdh:john_father")` | `jdh:john_father/neutral` | ок |
| `family\sasha_neutral.face-pose.es` | `FacePose("jdh:sasha/neutral", rig: "jdh:sasha")` | `jdh:sasha/neutral` | ок |
| `friends\ben_neutral.face-pose.es` | `FacePose("jdh:ben/neutral", rig: "jdh:ben")` | `jdh:ben/neutral` | ок |
| `friends\dinara_neutral.face-pose.es` | `FacePose("jdh:dinara/neutral", rig: "jdh:dinara")` | `jdh:dinara/neutral` | ок |
| `friends\natan_neutral.face-pose.es` | `FacePose("jdh:natan/neutral", rig: "jdh:natan")` | `jdh:natan/neutral` | ок |
| `friends\sofa_neutral.face-pose.es` | `FacePose("jdh:natan/neutral", rig: "jdh:natan")` | `jdh:sofa/neutral` | подозрительное копирование |
| `work\molly_neutral.face-pose.es` | `FacePose("jdh:john_father/neutral", rig: "jdh:molly")` | `jdh:molly/neutral` | подозрительное копирование |

Исправленная нейтральная поза Molly должна выглядеть так:

```csharp
namespace JDH.Animation.Chapter1.Work;

class MollyNeutralFacePose : FacePoseResource
{
    public override FacePoseDef Build()
    {
        return FacePose("jdh:molly/neutral", rig: "jdh:molly",
            bones: [
                BoneDelta("face_root")
            ],
            controls: [
                ControlDelta("eye_closure", value: 0)
            ]
        );
    }
}
```

Исправленная нейтральная поза Sofa должна выглядеть так:

```csharp
namespace JDH.Animation.Chapter1.Friends;

class SofaNeutralFacePose : FacePoseResource
{
    public override FacePoseDef Build()
    {
        return FacePose("jdh:sofa/neutral", rig: "jdh:sofa",
            bones: [
                BoneDelta("face_root")
            ],
            controls: [
                ControlDelta("eye_closure", value: 0)
            ]
        );
    }
}
```

Продакшен лицевые facial-анимации в runtime-паке:

| ID | Риг | Файл |
|---|---|---|
| `jdh:john_father/angry_brows` | `jdh:john_father` | `data\jdh\echo_animation\face_animations\chapter_1\family\john_father\angry_brows.face-animation.es` |
| `jdh:john_father/brows_raise` | `jdh:john_father` | `data\jdh\echo_animation\face_animations\chapter_1\family\john_father\brows_raise.face-animation.es` |
| `jdh:john_father/full_face_hold` | `jdh:john_father` | `data\jdh\echo_animation\face_animations\chapter_1\family\john_father\full_face_hold.face-animation.es` |
| `jdh:john_father/look_left` | `jdh:john_father` | `data\jdh\echo_animation\face_animations\chapter_1\family\john_father\look_left.face-animation.es` |
| `jdh:molly/angry_brows` | `jdh:molly` | `data\jdh\echo_animation\face_animations\chapter_1\work\molly\angry_brows.face-animation.es` |
| `jdh:molly/brows_raise` | `jdh:molly` | `data\jdh\echo_animation\face_animations\chapter_1\work\molly\brows_raise.face-animation.es` |
| `jdh:molly/full_face_hold` | `jdh:molly` | `data\jdh\echo_animation\face_animations\chapter_1\work\molly\full_face_hold.face-animation.es` |
| `jdh:molly/look_left` | `jdh:molly` | `data\jdh\echo_animation\face_animations\chapter_1\work\molly\look_left.face-animation.es` |
| `jdh:player/brows_raise` | `echo_content_player:player/base` | `data\jdh\echo_animation\face_animations\chapter_1\player\brows_raise.face-animation.es` |
| `jdh:player/look_left` | `echo_content_player:player/base` | `data\jdh\echo_animation\face_animations\chapter_1\player\look_left.face-animation.es` |
| `jdh:sasha/sleep` | `jdh:sasha` | `data\jdh\echo_animation\face_animations\chapter_1\family\sasha\sleep.face-animation.es` |

Тестовые и общие лицевые facial-анимации в runtime-паке:

```text
jdh:shared/blink
jdh:face_test/look_left
jdh:face_test/face_nudge
jdh:face_test/brows_raise
jdh:face_test/angry_brows
jdh:face_test/blink
jdh:face_test/full_face_hold
```

Runtime NPC:

| ID NPC | Отображаемое имя | Скин |
|---|---|---|
| `jdh:john_father` | `Отец` | `Skin("jdh:john_father")` |
| `jdh:sasha` | `Саша` | `Skin("jdh:sasha")` |
| `jdh:ben` | `Бен` | `Skin("jdh:ben")` |
| `jdh:dinara` | `Динара` | `Skin("jdh:dinara")` |
| `jdh:natan` | `Натан` | `Skin("jdh:natan")` |
| `jdh:sofa` | `Софа` | `Skin("jdh:sofa")` |
| `jdh:molly` | `Молли` | `Skin("jdh:molly")` |

Runtime PNG-скины NPC:

```text
assets\jdh\textures\echo_npc\skins\ben.png
assets\jdh\textures\echo_npc\skins\content_pack_father.png
assets\jdh\textures\echo_npc\skins\dinara.png
assets\jdh\textures\echo_npc\skins\john_father.png
assets\jdh\textures\echo_npc\skins\molly.png
assets\jdh\textures\echo_npc\skins\natan.png
assets\jdh\textures\echo_npc\skins\sasha.png
assets\jdh\textures\echo_npc\skins\sofa.png
```

Runtime визуальные ассеты диалогов:

```text
assets\jdh\textures\echo_dialogue\portraits\john_father.png
assets\jdh\textures\echo_dialogue\skins\ben.png
assets\jdh\textures\echo_dialogue\skins\dinara.png
assets\jdh\textures\echo_dialogue\skins\john_father.png
assets\jdh\textures\echo_dialogue\skins\molly.png
assets\jdh\textures\echo_dialogue\skins\natan.png
assets\jdh\textures\echo_dialogue\skins\sasha.png
assets\jdh\textures\echo_dialogue\skins\sofa.png
```

Runtime-привязки NPC к анимациям:

```text
data\jdh\echo_npc_animation\bindings\chapter_1\family\john_father.npc-animation-binding.es
echo_npc_animation\bindings\chapter_1\family\molly.npc-animation-binding.es
echo_npc_animation\bindings\chapter_1\family\john_father.npc-animation-binding — копия.es
```

`molly` binding сейчас лежит в структуре внешнего сканера верхнего уровня:

```csharp
namespace JDH.NpcAnimation.Chapter1.Work;

class MollyNpcAnimationBinding : NpcAnimationBindingResource
{
    public override NpcAnimationBindingDef Build()
    {
        return NpcAnimationBinding("jdh:molly_standard_humanoid",
            npc: "jdh:molly",
            profile: "jdh:standard_humanoid"
        );
    }
}
```

`john_father` binding есть и в `data\jdh\...`, и как копия верхнего уровня. Если загрузятся оба, `NpcAnimationBindingCompiler` может сообщить:

```text
EANIM-A4-BINDING Duplicate definition binding: jdh:john_father
```

потому что поиск дублей идет по `npc`/`definitionId`, а не по имени файла.

Runtime-ресурсы body-анимаций в `data\jdh\echo_animation`:

```text
animation_rigs\chapter_1\family\humanoid.animation-rig.es
animation_profiles\chapter_1\family\standard_humanoid.animation-profile.es
animations\chapter_1\family\wave.animation.es
animations\chapter_1\family\look_around_once.animation.es
animations\chapter_1\family\knee_bend.animation.es
animations\chapter_1\family\humanoid_walk.animation.es
```

Также есть похожий дублирующий профиль верхнего уровня:

```text
echo_animation\animation_profiles\chapter_1\family\standard_humanoid.animation-profile.es
```

Если профиль из datapack и профиль верхнего уровня оба загрузятся с одинаковым `AnimationProfile("jdh:standard_humanoid", ...)`, `AnimationProfileCandidateValidator` может сообщить:

```text
EANIM-FORGE-PROFILE Duplicate animation profile id: jdh:standard_humanoid
```

### Несовпадения overlay-слоёв Prism

`john_father` face rig содержит `Bone Glasses` и `assets\...\faces\john_father\glasses.png`, но runtime `john_father.portrait-overlay.es` не содержит `Layer("glasses", ...)`. Итог: очки могут существовать в face-rig/3D, но не рисоваться в портретном overlay, если их не рисует другая система.

Если нужно, добавьте очки в портретный overlay отца:

```csharp
Layer("glasses", placeX: "0", placeY: "0", scaleX: "1", scaleY: "1")
```

`molly` face rig содержит `Bone Glasses`, `glasses.png`, и overlay содержит `Layer("glasses", ...)`. Этот набор согласован.

`ben` и `natan` overlay-конфиги содержат `Layer("glasses", ...)`, но их face-rig-и не объявляют `Bone Glasses`, и их папки текстур лица не содержат `glasses.png`. Есть два чистых варианта исправления.

Вариант A: убрать слой очков из overlay:

```csharp
// ???pose at? portrait overlay Ben/Natan, pose at?? ?? ??pose at?pose at???.
Layer("glasses", placeX: "0", placeY: "0.06", scaleX: "1", scaleY: "1")
```

Вариант B: добавить поддержку очков в риг и папку текстур:

```csharp
// ? face Ben("jdh:ben") ??? face Natan("jdh:natan")
Bone Glasses
{
    Position(0, 0.06, 0.04);
    Size(6, 2);
}
```

Нужный ассет:

```text
assets\jdh\textures\echo_animation\faces\ben\glasses.png
assets\jdh\textures\echo_animation\faces\natan\glasses.png
```

## Больше готовых примеров кода

Этот раздел сделан как copy-paste база для сценаристов и разработчиков контента. Примеры специально длиннее обычного: в них видно, какой файл куда класть, какой ID использовать в диалоге, что за что отвечает, и какие места чаще всего ломаются при копировании персонажа.

### Переменные Prism для PowerShell-проверок

Удобно сначала завести переменные, чтобы не переписывать длинный путь с кириллицей:

```powershell
$JdhRepo = "D:\JDHSeason"
$JdhDocs = "D:\JDHSeason\docs\markdown"
$PrismPack = "C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\content-pack\jdh"
$PrismConfig = "C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\configs\jdh"

Test-Path $PrismPack
Test-Path $PrismConfig
Get-ChildItem $PrismPack
Get-ChildItem $PrismConfig
```

Быстрый поиск всех мест, где используется конкретная лицевая facial-анимация:

```powershell
rg -n "jdh:john_father/angry_brows|jdh:molly/brows_raise|jdh:sasha/sleep" $PrismPack $PrismConfig "$JdhRepo\story"
```

Быстрый поиск дублей ID в content-pack:

```powershell
rg -n "FacePose\\(\"jdh:john_father/neutral\"|FacePose\\(\"jdh:natan/neutral\"|AnimationProfile\\(\"jdh:standard_humanoid\"|NpcAnimationBinding\\(\"jdh:john_father\"" $PrismPack
```

Проверка, какие `.es` файлы вообще есть в runtime-паке:

```powershell
Get-ChildItem $PrismPack -Recurse -Filter *.es |
    Select-Object FullName |
    Sort-Object FullName
```

### Шаблон лицевого рига (face rig): обычный персонаж

Файл:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\content-pack\jdh\data\jdh\echo_animation\face_rigs\chapter_1\friends\new_character.face-rig.es
```

Пример рига без очков. Это шаблон в стиле `ben`, `dinara`, `natan`, `sasha`, `sofa`.

```csharp
face NewCharacter("jdh:new_character")
{
    Textures("jdh:echo_animation/faces/new_character");

    Eyes
    {
        Sclera Left
        {
            Position(-0.125, 0.060, 0.000);
            Size(2, 2);
            Texture("sclera_left");
        }

        Sclera Right
        {
            Position(0.125, 0.060, 0.000);
            Size(2, 2);
            Texture("sclera_right");
        }

        Pupil Left
        {
            Position(-0.090, 0.000, 0.010);
            Size(1, 1);
            Texture("pupil_left");
        }

        Pupil Right
        {
            Position(0.090, 0.000, 0.010);
            Size(1, 1);
            Texture("pupil_right");
        }

        Eyelid Upper
        {
            Position(0.000, -0.035, 0.020);
            TextureArea(8, 13, 8, 1);
            Texture("upper_eyelid");
        }

        Eyelid Lower
        {
            Position(0.000, 0.153, 0.020);
            TextureArea(8, 12, 8, 1);
            Texture("lower_eyelid");
        }
    }

    Brows
    {
        Pivot(-62, 0);

        Left
        {
            Position(-0.125, -0.032, 0.030);
            Size(2, 1);
            Texture("brows_left");
        }

        Right
        {
            Position(0.124, -0.032, 0.030);
            Pivot(62, 0);
            Size(2, 1);
            Texture("brows_right");
        }
    }

    Blink "eye_closure"
    {
        Travel(65, -61);
        Gaze(1, 1);
        Idle(true);
        PeriodTicks(60);
        CloseTicks(3);
        HoldTicks(3);
        OpenTicks(3);
    }
}
```

Что здесь важно:

- `face NewCharacter("jdh:new_character")` - главный ID рига. Его потом указывают в `FacePose(..., rig: "jdh:new_character")` и `FacialAnimation(..., rig: "jdh:new_character")`.
- `Textures("jdh:echo_animation/faces/new_character")` - папка с PNG слоями лица.
- `Sclera`, `Pupil`, `Eyelid`, `Brows` - логические группы костей лица.
- `Position(x, y, z)` - локальное смещение слоя. Для лица маленькие числа вроде `0.06`, `0.153` нормальны.
- `z` помогает разнести слои по глубине: белки глаз ниже, зрачки выше, веки выше зрачков, брови еще выше.
- `Blink "eye_closure"` задает idle-моргание. Если facial-анимация ставит `suppressIdleBlink: true`, это idle-моргание временно подавляется.

Минимальные PNG для такого рига:

```text
assets\jdh\textures\echo_animation\faces\new_character\sclera_left.png
assets\jdh\textures\echo_animation\faces\new_character\sclera_right.png
assets\jdh\textures\echo_animation\faces\new_character\pupil_left.png
assets\jdh\textures\echo_animation\faces\new_character\pupil_right.png
assets\jdh\textures\echo_animation\faces\new_character\upper_eyelid_left.png
assets\jdh\textures\echo_animation\faces\new_character\upper_eyelid_right.png
assets\jdh\textures\echo_animation\faces\new_character\lower_eyelid_left.png
assets\jdh\textures\echo_animation\faces\new_character\lower_eyelid_right.png
assets\jdh\textures\echo_animation\faces\new_character\brows_left.png
assets\jdh\textures\echo_animation\faces\new_character\brows_right.png
```

Если используется общий `upper_eyelid.png`, а рендер ожидает `upper_eyelid_left.png` и `upper_eyelid_right.png`, слой может не отрисоваться. Проверяйте реальные имена файлов в папке персонажа.

### Шаблон лицевого рига (face rig): персонаж с очками

Файл:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\content-pack\jdh\data\jdh\echo_animation\face_rigs\chapter_1\friends\new_character_glasses.face-rig.es
```

Отличие от обычного рига - дополнительная кость `Bone Glasses` и файл `glasses.png`.

```csharp
face NewCharacterGlasses("jdh:new_character_glasses")
{
    Textures("jdh:echo_animation/faces/new_character_glasses");

    Eyes
    {
        Sclera Left  { Position(-0.125, 0.060, 0.000); Size(2, 2); Texture("sclera_left"); }
        Sclera Right { Position( 0.125, 0.060, 0.000); Size(2, 2); Texture("sclera_right"); }

        Pupil Left   { Position(-0.090, 0.000, 0.010); Size(1, 1); Texture("pupil_left"); }
        Pupil Right  { Position( 0.090, 0.000, 0.010); Size(1, 1); Texture("pupil_right"); }

        Eyelid Upper { Position(0.000, -0.035, 0.020); TextureArea(8, 13, 8, 1); }
        Eyelid Lower { Position(0.000,  0.153, 0.020); TextureArea(8, 12, 8, 1); }
    }

    Brows
    {
        Pivot(-62, 0);
        Left  { Position(-0.125, -0.032, 0.030); Size(2, 1); Texture("brows_left"); }
        Right { Position( 0.124, -0.032, 0.030); Pivot(62, 0); Size(2, 1); Texture("brows_right"); }
    }

    Bone Glasses
    {
        Position(0.000, 0.060, 0.040);
        Size(6, 2);
        Texture("glasses");
    }

    Blink "eye_closure"
    {
        Travel(65, -61);
        Gaze(1, 1);
        Idle(true);
        PeriodTicks(60);
        CloseTicks(3);
        HoldTicks(3);
        OpenTicks(3);
    }
}
```

Для портрета очки надо добавить отдельно в `.portrait-overlay.es`, потому что overlay не обязан автоматически повторять все кости 3D-рига:

```csharp
Layer("glasses", placeX: "0", placeY: "0.06", scaleX: "1", scaleY: "1")
```

### Поза лица (Face pose): нейтральное лицо

Файл:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\content-pack\jdh\data\jdh\echo_animation\face_poses\chapter_1\friends\new_character_neutral.face-pose.es
```

```csharp
using mods.echo_animation.resources.face;

class NewCharacterNeutralFacePose : FacePoseResource
{
    public override FacePoseDef Build()
    {
        return FacePose("jdh:new_character/neutral", rig: "jdh:new_character")
            .Layer("sclera_left").At(-0.125, 0.060, 0.000).Done()
            .Layer("sclera_right").At(0.125, 0.060, 0.000).Done()
            .Layer("pupil_left").At(-0.090, 0.000, 0.010).Done()
            .Layer("pupil_right").At(0.090, 0.000, 0.010).Done()
            .Layer("upper_eyelid_left").At(-0.125, -0.035, 0.020).Done()
            .Layer("upper_eyelid_right").At(0.125, -0.035, 0.020).Done()
            .Layer("lower_eyelid_left").At(-0.125, 0.153, 0.020).Done()
            .Layer("lower_eyelid_right").At(0.125, 0.153, 0.020).Done()
            .Layer("brows_left").At(-0.125, -0.032, 0.030).Done()
            .Layer("brows_right").At(0.124, -0.032, 0.030).Done();
    }
}
```

Правило копирования: ID позы и `rig` должны принадлежать одному персонажу.

```csharp
// Хорошо:
FacePose("jdh:molly/neutral", rig: "jdh:molly")

// Плохо: ID от отца, rig от Molly. Валидатор может пропустить часть структуры,
// но сценарист потом не найдет ожидаемый jdh:molly/neutral.
FacePose("jdh:john_father/neutral", rig: "jdh:molly")
```

### Шаблон портретного overlay

Файл:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\configs\jdh\echo_animation\portrait_overlays\new_character.portrait-overlay.es
```

```csharp
using mods.echo_animation.client.portrait;

class NewCharacterPortraitOverlay : PortraitOverlayResource
{
    public override PortraitOverlayDef Build()
    {
        return PortraitOverlay("jdh:new_character")
        {
            TextureRoot = "jdh:echo_animation/faces/new_character",
            CenterX = 0.50,
            CenterY = 0.50,
            FacePixels = 8,

            Layers =
            [
                Layer("sclera_left",        placeX: "-0.125", placeY: "0.06",  scaleX: "1", scaleY: "1"),
                Layer("sclera_right",       placeX: "0.125",  placeY: "0.06",  scaleX: "1", scaleY: "1"),
                Layer("pupil_left",         placeX: "-0.125", placeY: "0.06",  scaleX: "1", scaleY: "1"),
                Layer("pupil_right",        placeX: "0.063",  placeY: "0.06",  scaleX: "1", scaleY: "1"),
                Layer("upper_eyelid_left",  placeX: "-0.125", placeY: "-0.065", scaleX: "1", scaleY: "1"),
                Layer("upper_eyelid_right", placeX: "0.125",  placeY: "-0.065", scaleX: "1", scaleY: "1"),
                Layer("lower_eyelid_left",  placeX: "-0.125", placeY: "0.123", scaleX: "1", scaleY: "1"),
                Layer("lower_eyelid_right", placeX: "0.125",  placeY: "0.123", scaleX: "1", scaleY: "1"),
                Layer("brows_left",         placeX: "-0.125", placeY: "-0.065", scaleX: "1", scaleY: "1"),
                Layer("brows_right",        placeX: "0.125",  placeY: "-0.065", scaleX: "1", scaleY: "1")
            ]
        };
    }
}
```

Что делает overlay:

- `PortraitOverlay("jdh:new_character")` привязывает портретный overlay к визуальному ID/персонажу.
- `TextureRoot` указывает папку PNG для слоев портрета.
- `CenterX`, `CenterY` задают центр лица внутри головы портрета.
- `FacePixels` задает масштаб пересчета маленьких координат лица в пиксели портрета.
- `Layer(...)` перечисляет порядок рисования. Если слой не перечислен, портрет его не нарисует.

### Лицевая анимация (Facial animation): взгляд вправо

Файл:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\content-pack\jdh\data\jdh\echo_animation\face_animations\chapter_1\friends\new_character_look_right.face-animation.es
```

ID для диалога:

```text
jdh:new_character/look_right
```

```csharp
using mods.echo_animation.resources.face;

class NewCharacterLookRightAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation(
            "jdh:new_character/look_right",
            rig: "jdh:new_character",
            suppressIdleBlink: false,
            surfaces: [World, Portrait]
        );
    }

    class World : FacialAnimationSurface3d
    {
        private PhaseDef Enter()
        {
            return Phase(durationTicks: 6, easing: "ease_out_quad")
                .Track("pupil_left").MoveTo(0.055, 0.000, 0.000).Done()
                .Track("pupil_right").MoveTo(0.055, 0.000, 0.000).Done();
        }

        private PhaseDef Hold()
        {
            return Phase(durationTicks: 20, easing: "hold")
                .Track("pupil_left").MoveTo(0.055, 0.000, 0.000).Done()
                .Track("pupil_right").MoveTo(0.055, 0.000, 0.000).Done();
        }

        private PhaseDef Exit()
        {
            return Phase(durationTicks: 6, easing: "ease_in_quad")
                .Track("pupil_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("pupil_right").MoveTo(0.000, 0.000, 0.000).Done();
        }
    }

    class Portrait : FacialAnimationSurfacePortrait
    {
        private PhaseDef Enter()
        {
            return Phase(durationTicks: 6, easing: "ease_out_quad")
                .Track("pupil_left").MoveTo(0.030, 0.000, 0.000).Done()
                .Track("pupil_right").MoveTo(0.030, 0.000, 0.000).Done();
        }

        private PhaseDef Hold()
        {
            return Phase(durationTicks: 20, easing: "hold")
                .Track("pupil_left").MoveTo(0.030, 0.000, 0.000).Done()
                .Track("pupil_right").MoveTo(0.030, 0.000, 0.000).Done();
        }

        private PhaseDef Exit()
        {
            return Phase(durationTicks: 6, easing: "ease_in_quad")
                .Track("pupil_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("pupil_right").MoveTo(0.000, 0.000, 0.000).Done();
        }
    }
}
```

Почему в `Portrait` значения меньше: портретный слой часто выглядит резче, чем 3D-слой. Маленькое смещение зрачков в портрете читается лучше и не вылезает за белки глаз.

### Лицевая анимация (Facial animation): разовое моргание

Файл:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\content-pack\jdh\data\jdh\echo_animation\face_animations\chapter_1\friends\new_character_blink_once.face-animation.es
```

```csharp
using mods.echo_animation.resources.face;

class NewCharacterBlinkOnceAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation(
            "jdh:new_character/blink_once",
            rig: "jdh:new_character",
            suppressIdleBlink: true,
            surfaces: [World, Portrait]
        );
    }

    class World : FacialAnimationSurface3d
    {
        private PhaseDef Close()
        {
            return Phase(durationTicks: 3, easing: "ease_in_quad")
                .Track("upper_eyelid_left").MoveTo(0.000, 0.070, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.070, 0.000).Done()
                .Track("lower_eyelid_left").MoveTo(0.000, -0.060, 0.000).Done()
                .Track("lower_eyelid_right").MoveTo(0.000, -0.060, 0.000).Done();
        }

        private PhaseDef Hold()
        {
            return Phase(durationTicks: 2, easing: "hold")
                .Track("upper_eyelid_left").MoveTo(0.000, 0.070, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.070, 0.000).Done()
                .Track("lower_eyelid_left").MoveTo(0.000, -0.060, 0.000).Done()
                .Track("lower_eyelid_right").MoveTo(0.000, -0.060, 0.000).Done();
        }

        private PhaseDef Open()
        {
            return Phase(durationTicks: 4, easing: "ease_out_quad")
                .Track("upper_eyelid_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("lower_eyelid_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("lower_eyelid_right").MoveTo(0.000, 0.000, 0.000).Done();
        }
    }

    class Portrait : FacialAnimationSurfacePortrait
    {
        private PhaseDef Close()
        {
            return Phase(durationTicks: 3, easing: "ease_in_quad")
                .Track("upper_eyelid_left").MoveTo(0.000, 0.050, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.050, 0.000).Done()
                .Track("lower_eyelid_left").MoveTo(0.000, -0.045, 0.000).Done()
                .Track("lower_eyelid_right").MoveTo(0.000, -0.045, 0.000).Done();
        }

        private PhaseDef Hold()
        {
            return Phase(durationTicks: 2, easing: "hold")
                .Track("upper_eyelid_left").MoveTo(0.000, 0.050, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.050, 0.000).Done()
                .Track("lower_eyelid_left").MoveTo(0.000, -0.045, 0.000).Done()
                .Track("lower_eyelid_right").MoveTo(0.000, -0.045, 0.000).Done();
        }

        private PhaseDef Open()
        {
            return Phase(durationTicks: 4, easing: "ease_out_quad")
                .Track("upper_eyelid_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("lower_eyelid_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("lower_eyelid_right").MoveTo(0.000, 0.000, 0.000).Done();
        }
    }
}
```

Использование: короткий blink хорош для реплик, где персонаж сбрасывает эмоцию или реагирует на странную фразу. Для сна лучше не использовать blink, а держать веки закрытыми отдельной анимацией.

### Лицевая анимация (Facial animation): сон или закрытые глаза

Файл:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\content-pack\jdh\data\jdh\echo_animation\face_animations\chapter_1\friends\new_character_sleep.face-animation.es
```

```csharp
using mods.echo_animation.resources.face;

class NewCharacterSleepAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation(
            "jdh:new_character/sleep",
            rig: "jdh:new_character",
            suppressIdleBlink: true,
            surfaces: [World, Portrait]
        );
    }

    class World : FacialAnimationSurface3d
    {
        private PhaseDef Enter()
        {
            return Phase(durationTicks: 5, easing: "ease_out_quad")
                .Track("upper_eyelid_left").MoveTo(0.000, 0.070, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.070, 0.000).Done()
                .Track("lower_eyelid_left").MoveTo(0.000, -0.060, 0.000).Done()
                .Track("lower_eyelid_right").MoveTo(0.000, -0.060, 0.000).Done();
        }

        private PhaseDef StayClosed()
        {
            return Phase(durationTicks: 40, easing: "hold")
                .Track("upper_eyelid_left").MoveTo(0.000, 0.070, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.070, 0.000).Done()
                .Track("lower_eyelid_left").MoveTo(0.000, -0.060, 0.000).Done()
                .Track("lower_eyelid_right").MoveTo(0.000, -0.060, 0.000).Done();
        }

        private PhaseDef Exit()
        {
            return Phase(durationTicks: 7, easing: "ease_out_quad")
                .Track("upper_eyelid_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("lower_eyelid_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("lower_eyelid_right").MoveTo(0.000, 0.000, 0.000).Done();
        }
    }

    class Portrait : FacialAnimationSurfacePortrait
    {
        private PhaseDef Enter()
        {
            return Phase(durationTicks: 5, easing: "ease_out_quad")
                .Track("upper_eyelid_left").MoveTo(0.000, 0.050, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.050, 0.000).Done()
                .Track("lower_eyelid_left").MoveTo(0.000, -0.045, 0.000).Done()
                .Track("lower_eyelid_right").MoveTo(0.000, -0.045, 0.000).Done();
        }

        private PhaseDef StayClosed()
        {
            return Phase(durationTicks: 40, easing: "hold")
                .Track("upper_eyelid_left").MoveTo(0.000, 0.050, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.050, 0.000).Done()
                .Track("lower_eyelid_left").MoveTo(0.000, -0.045, 0.000).Done()
                .Track("lower_eyelid_right").MoveTo(0.000, -0.045, 0.000).Done();
        }

        private PhaseDef Exit()
        {
            return Phase(durationTicks: 7, easing: "ease_out_quad")
                .Track("upper_eyelid_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("lower_eyelid_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("lower_eyelid_right").MoveTo(0.000, 0.000, 0.000).Done();
        }
    }
}
```

Для диалога-сна обычно лучше держать эту лицевую facial-анимацию на нескольких строках и сбросить ее вручную:

```csharp
var Sleep = "jdh:new_character/sleep";

Line("new_character", "...").SetAnimationFace(Sleep);
Line("john", "Она, кажется, спит.");
Line("new_character", "М-м...");
Line("john", "Не буду будить.");
StopFaceAnimation("new_character");
```

### Лицевая анимация (Facial animation): удивление всем лицом

Файл:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\content-pack\jdh\data\jdh\echo_animation\face_animations\chapter_1\friends\new_character_surprise.face-animation.es
```

```csharp
using mods.echo_animation.resources.face;

class NewCharacterSurpriseAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation(
            "jdh:new_character/surprise",
            rig: "jdh:new_character",
            suppressIdleBlink: false,
            surfaces: [World, Portrait]
        );
    }

    class World : FacialAnimationSurface3d
    {
        private PhaseDef Enter()
        {
            return Phase(durationTicks: 5, easing: "ease_out_back")
                .Track("brows_left").MoveTo(0.000, -0.035, 0.000).RotateTo(0.000, 0.000, -7.000).Done()
                .Track("brows_right").MoveTo(0.000, -0.035, 0.000).RotateTo(0.000, 0.000, 7.000).Done()
                .Track("upper_eyelid_left").MoveTo(0.000, -0.030, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, -0.030, 0.000).Done()
                .Track("pupil_left").MoveTo(0.000, -0.010, 0.000).Done()
                .Track("pupil_right").MoveTo(0.000, -0.010, 0.000).Done();
        }

        private PhaseDef Hold()
        {
            return Phase(durationTicks: 18, easing: "hold")
                .Track("brows_left").MoveTo(0.000, -0.035, 0.000).RotateTo(0.000, 0.000, -7.000).Done()
                .Track("brows_right").MoveTo(0.000, -0.035, 0.000).RotateTo(0.000, 0.000, 7.000).Done()
                .Track("upper_eyelid_left").MoveTo(0.000, -0.030, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, -0.030, 0.000).Done()
                .Track("pupil_left").MoveTo(0.000, -0.010, 0.000).Done()
                .Track("pupil_right").MoveTo(0.000, -0.010, 0.000).Done();
        }

        private PhaseDef Exit()
        {
            return Phase(durationTicks: 8, easing: "ease_in_out_quad")
                .Track("brows_left").MoveTo(0.000, 0.000, 0.000).RotateTo(0.000, 0.000, 0.000).Done()
                .Track("brows_right").MoveTo(0.000, 0.000, 0.000).RotateTo(0.000, 0.000, 0.000).Done()
                .Track("upper_eyelid_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("pupil_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("pupil_right").MoveTo(0.000, 0.000, 0.000).Done();
        }
    }

    class Portrait : FacialAnimationSurfacePortrait
    {
        private PhaseDef Enter()
        {
            return Phase(durationTicks: 5, easing: "ease_out_back")
                .Track("brows_left").MoveTo(0.000, -0.020, 0.000).RotateTo(0.000, 0.000, -5.000).Done()
                .Track("brows_right").MoveTo(0.000, -0.020, 0.000).RotateTo(0.000, 0.000, 5.000).Done()
                .Track("upper_eyelid_left").MoveTo(0.000, -0.020, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, -0.020, 0.000).Done()
                .Track("pupil_left").MoveTo(0.000, -0.006, 0.000).Done()
                .Track("pupil_right").MoveTo(0.000, -0.006, 0.000).Done();
        }

        private PhaseDef Hold()
        {
            return Phase(durationTicks: 18, easing: "hold")
                .Track("brows_left").MoveTo(0.000, -0.020, 0.000).RotateTo(0.000, 0.000, -5.000).Done()
                .Track("brows_right").MoveTo(0.000, -0.020, 0.000).RotateTo(0.000, 0.000, 5.000).Done()
                .Track("upper_eyelid_left").MoveTo(0.000, -0.020, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, -0.020, 0.000).Done()
                .Track("pupil_left").MoveTo(0.000, -0.006, 0.000).Done()
                .Track("pupil_right").MoveTo(0.000, -0.006, 0.000).Done();
        }

        private PhaseDef Exit()
        {
            return Phase(durationTicks: 8, easing: "ease_in_out_quad")
                .Track("brows_left").MoveTo(0.000, 0.000, 0.000).RotateTo(0.000, 0.000, 0.000).Done()
                .Track("brows_right").MoveTo(0.000, 0.000, 0.000).RotateTo(0.000, 0.000, 0.000).Done()
                .Track("upper_eyelid_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("pupil_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("pupil_right").MoveTo(0.000, 0.000, 0.000).Done();
        }
    }
}
```

Сюжетное применение: персонаж услышал невозможную информацию, резко поднял брови, глаза раскрылись. Подходит для одной реплики или короткой паузы, но не для долгой сцены.

### Лицевая анимация (Facial animation): подозрение или прищур

Файл:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\content-pack\jdh\data\jdh\echo_animation\face_animations\chapter_1\friends\new_character_squint.face-animation.es
```

```csharp
using mods.echo_animation.resources.face;

class NewCharacterSquintAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation(
            "jdh:new_character/squint",
            rig: "jdh:new_character",
            suppressIdleBlink: false,
            surfaces: [World, Portrait]
        );
    }

    class World : FacialAnimationSurface3d
    {
        private PhaseDef Enter()
        {
            return Phase(durationTicks: 7, easing: "ease_out_quad")
                .Track("upper_eyelid_left").MoveTo(0.000, 0.035, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.035, 0.000).Done()
                .Track("lower_eyelid_left").MoveTo(0.000, -0.025, 0.000).Done()
                .Track("lower_eyelid_right").MoveTo(0.000, -0.025, 0.000).Done()
                .Track("brows_left").MoveTo(0.012, 0.012, 0.000).RotateTo(0.000, 0.000, 8.000).Done()
                .Track("brows_right").MoveTo(-0.012, 0.012, 0.000).RotateTo(0.000, 0.000, -8.000).Done();
        }

        private PhaseDef Hold()
        {
            return Phase(durationTicks: 30, easing: "hold")
                .Track("upper_eyelid_left").MoveTo(0.000, 0.035, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.035, 0.000).Done()
                .Track("lower_eyelid_left").MoveTo(0.000, -0.025, 0.000).Done()
                .Track("lower_eyelid_right").MoveTo(0.000, -0.025, 0.000).Done()
                .Track("brows_left").MoveTo(0.012, 0.012, 0.000).RotateTo(0.000, 0.000, 8.000).Done()
                .Track("brows_right").MoveTo(-0.012, 0.012, 0.000).RotateTo(0.000, 0.000, -8.000).Done();
        }

        private PhaseDef Exit()
        {
            return Phase(durationTicks: 8, easing: "ease_out_quad")
                .Track("upper_eyelid_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("lower_eyelid_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("lower_eyelid_right").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("brows_left").MoveTo(0.000, 0.000, 0.000).RotateTo(0.000, 0.000, 0.000).Done()
                .Track("brows_right").MoveTo(0.000, 0.000, 0.000).RotateTo(0.000, 0.000, 0.000).Done();
        }
    }

    class Portrait : FacialAnimationSurfacePortrait
    {
        private PhaseDef Enter()
        {
            return Phase(durationTicks: 7, easing: "ease_out_quad")
                .Track("upper_eyelid_left").MoveTo(0.000, 0.022, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.022, 0.000).Done()
                .Track("lower_eyelid_left").MoveTo(0.000, -0.018, 0.000).Done()
                .Track("lower_eyelid_right").MoveTo(0.000, -0.018, 0.000).Done()
                .Track("brows_left").MoveTo(0.008, 0.008, 0.000).RotateTo(0.000, 0.000, 5.000).Done()
                .Track("brows_right").MoveTo(-0.008, 0.008, 0.000).RotateTo(0.000, 0.000, -5.000).Done();
        }

        private PhaseDef Hold()
        {
            return Phase(durationTicks: 30, easing: "hold")
                .Track("upper_eyelid_left").MoveTo(0.000, 0.022, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.022, 0.000).Done()
                .Track("lower_eyelid_left").MoveTo(0.000, -0.018, 0.000).Done()
                .Track("lower_eyelid_right").MoveTo(0.000, -0.018, 0.000).Done()
                .Track("brows_left").MoveTo(0.008, 0.008, 0.000).RotateTo(0.000, 0.000, 5.000).Done()
                .Track("brows_right").MoveTo(-0.008, 0.008, 0.000).RotateTo(0.000, 0.000, -5.000).Done();
        }

        private PhaseDef Exit()
        {
            return Phase(durationTicks: 8, easing: "ease_out_quad")
                .Track("upper_eyelid_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("lower_eyelid_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("lower_eyelid_right").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("brows_left").MoveTo(0.000, 0.000, 0.000).RotateTo(0.000, 0.000, 0.000).Done()
                .Track("brows_right").MoveTo(0.000, 0.000, 0.000).RotateTo(0.000, 0.000, 0.000).Done();
        }
    }
}
```

### Лицевая анимация (Facial animation): печаль

Файл:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\content-pack\jdh\data\jdh\echo_animation\face_animations\chapter_1\friends\new_character_sad.face-animation.es
```

```csharp
using mods.echo_animation.resources.face;

class NewCharacterSadAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation(
            "jdh:new_character/sad",
            rig: "jdh:new_character",
            suppressIdleBlink: false,
            surfaces: [World, Portrait]
        );
    }

    class World : FacialAnimationSurface3d
    {
        private PhaseDef Enter()
        {
            return Phase(durationTicks: 10, easing: "ease_out_quad")
                .Track("brows_left").MoveTo(0.018, 0.018, 0.000).RotateTo(0.000, 0.000, -10.000).Done()
                .Track("brows_right").MoveTo(-0.018, 0.018, 0.000).RotateTo(0.000, 0.000, 10.000).Done()
                .Track("upper_eyelid_left").MoveTo(0.000, 0.012, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.012, 0.000).Done()
                .Track("pupil_left").MoveTo(0.000, 0.010, 0.000).Done()
                .Track("pupil_right").MoveTo(0.000, 0.010, 0.000).Done();
        }

        private PhaseDef Hold()
        {
            return Phase(durationTicks: 50, easing: "hold")
                .Track("brows_left").MoveTo(0.018, 0.018, 0.000).RotateTo(0.000, 0.000, -10.000).Done()
                .Track("brows_right").MoveTo(-0.018, 0.018, 0.000).RotateTo(0.000, 0.000, 10.000).Done()
                .Track("upper_eyelid_left").MoveTo(0.000, 0.012, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.012, 0.000).Done()
                .Track("pupil_left").MoveTo(0.000, 0.010, 0.000).Done()
                .Track("pupil_right").MoveTo(0.000, 0.010, 0.000).Done();
        }

        private PhaseDef Exit()
        {
            return Phase(durationTicks: 12, easing: "ease_out_quad")
                .Track("brows_left").MoveTo(0.000, 0.000, 0.000).RotateTo(0.000, 0.000, 0.000).Done()
                .Track("brows_right").MoveTo(0.000, 0.000, 0.000).RotateTo(0.000, 0.000, 0.000).Done()
                .Track("upper_eyelid_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("pupil_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("pupil_right").MoveTo(0.000, 0.000, 0.000).Done();
        }
    }

    class Portrait : FacialAnimationSurfacePortrait
    {
        private PhaseDef Enter()
        {
            return Phase(durationTicks: 10, easing: "ease_out_quad")
                .Track("brows_left").MoveTo(0.010, 0.010, 0.000).RotateTo(0.000, 0.000, -7.000).Done()
                .Track("brows_right").MoveTo(-0.010, 0.010, 0.000).RotateTo(0.000, 0.000, 7.000).Done()
                .Track("upper_eyelid_left").MoveTo(0.000, 0.008, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.008, 0.000).Done()
                .Track("pupil_left").MoveTo(0.000, 0.006, 0.000).Done()
                .Track("pupil_right").MoveTo(0.000, 0.006, 0.000).Done();
        }

        private PhaseDef Hold()
        {
            return Phase(durationTicks: 50, easing: "hold")
                .Track("brows_left").MoveTo(0.010, 0.010, 0.000).RotateTo(0.000, 0.000, -7.000).Done()
                .Track("brows_right").MoveTo(-0.010, 0.010, 0.000).RotateTo(0.000, 0.000, 7.000).Done()
                .Track("upper_eyelid_left").MoveTo(0.000, 0.008, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.008, 0.000).Done()
                .Track("pupil_left").MoveTo(0.000, 0.006, 0.000).Done()
                .Track("pupil_right").MoveTo(0.000, 0.006, 0.000).Done();
        }

        private PhaseDef Exit()
        {
            return Phase(durationTicks: 12, easing: "ease_out_quad")
                .Track("brows_left").MoveTo(0.000, 0.000, 0.000).RotateTo(0.000, 0.000, 0.000).Done()
                .Track("brows_right").MoveTo(0.000, 0.000, 0.000).RotateTo(0.000, 0.000, 0.000).Done()
                .Track("upper_eyelid_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("upper_eyelid_right").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("pupil_left").MoveTo(0.000, 0.000, 0.000).Done()
                .Track("pupil_right").MoveTo(0.000, 0.000, 0.000).Done();
        }
    }
}
```

### Лицевая анимация (Facial animation): злые брови

Файл:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\content-pack\jdh\data\jdh\echo_animation\face_animations\chapter_1\friends\new_character_angry_brows.face-animation.es
```

```csharp
using mods.echo_animation.resources.face;

class NewCharacterAngryBrowsAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation(
            "jdh:new_character/angry_brows",
            rig: "jdh:new_character",
            suppressIdleBlink: false,
            surfaces: [World, Portrait]
        );
    }

    class World : FacialAnimationSurface3d
    {
        private PhaseDef Enter()
        {
            return Phase(durationTicks: 5, easing: "ease_out_quad")
                .Track("brows_left").MoveTo(0.020, 0.018, 0.000).RotateTo(0.000, 0.000, 18.000).Done()
                .Track("brows_right").MoveTo(-0.020, 0.018, 0.000).RotateTo(0.000, 0.000, -18.000).Done();
        }

        private PhaseDef Hold()
        {
            return Phase(durationTicks: 26, easing: "hold")
                .Track("brows_left").MoveTo(0.020, 0.018, 0.000).RotateTo(0.000, 0.000, 18.000).Done()
                .Track("brows_right").MoveTo(-0.020, 0.018, 0.000).RotateTo(0.000, 0.000, -18.000).Done();
        }

        private PhaseDef Exit()
        {
            return Phase(durationTicks: 7, easing: "ease_in_quad")
                .Track("brows_left").MoveTo(0.000, 0.000, 0.000).RotateTo(0.000, 0.000, 0.000).Done()
                .Track("brows_right").MoveTo(0.000, 0.000, 0.000).RotateTo(0.000, 0.000, 0.000).Done();
        }
    }

    class Portrait : FacialAnimationSurfacePortrait
    {
        private PhaseDef Enter()
        {
            return Phase(durationTicks: 5, easing: "ease_out_quad")
                .Track("brows_left").MoveTo(0.010, 0.008, 0.000).RotateTo(0.000, 0.000, 10.000).Done()
                .Track("brows_right").MoveTo(-0.010, 0.008, 0.000).RotateTo(0.000, 0.000, -10.000).Done();
        }

        private PhaseDef Hold()
        {
            return Phase(durationTicks: 26, easing: "hold")
                .Track("brows_left").MoveTo(0.010, 0.008, 0.000).RotateTo(0.000, 0.000, 10.000).Done()
                .Track("brows_right").MoveTo(-0.010, 0.008, 0.000).RotateTo(0.000, 0.000, -10.000).Done();
        }

        private PhaseDef Exit()
        {
            return Phase(durationTicks: 7, easing: "ease_in_quad")
                .Track("brows_left").MoveTo(0.000, 0.000, 0.000).RotateTo(0.000, 0.000, 0.000).Done()
                .Track("brows_right").MoveTo(0.000, 0.000, 0.000).RotateTo(0.000, 0.000, 0.000).Done();
        }
    }
}
```

### Примеры диалогов (Dialogue): эмоции по репликам

Пример короткой эмоциональной сцены. Здесь лицевые facial-анимации меняются не сами по себе, а вместе с конкретными репликами.

```csharp
var FatherAngry = "jdh:john_father/angry_brows";
var FatherLookLeft = "jdh:john_father/look_left";
var PlayerLookLeft = "echo_content_player:player/look_left";
var SashaSleep = "jdh:sasha/sleep";

Line("john_father", "Ты опять пришел поздно.").SetAnimationFace(FatherAngry);
Line("john", "Я могу объяснить.").SetAnimationFace(PlayerLookLeft);
Line("john_father", "Объяснения будут завтра.").SetAnimationFace(FatherLookLeft);

StopFaceAnimation("john_father");
StopFaceAnimation("john");

Line("sasha", "...").SetAnimationFace(SashaSleep);
Line("john", "Саша даже не проснулась.");
StopFaceAnimation("sasha");
```

Практический смысл:

- `SetAnimationFace(...)` ставится на реплику, когда выражение должно появиться вместе с текстом.
- `StopFaceAnimation("alias")` нужен, если выражение должно гарантированно сброситься до следующего эмоционального блока.
- Для сна, злости, сильного удивления лучше явно сбрасывать эмоцию, потому что они визуально заметны.
- Для коротких взглядов можно разрешить им самим закончиться, если анимация короткая и в следующей строке нет конфликта.

### Примеры диалогов (Dialogue): одна эмоция на несколько реплик

Такой паттерн удобен для долгой реакции: персонаж злится не одну строку, а весь мини-диалог.

```csharp
var Angry = "jdh:john_father/angry_brows";
var CalmLook = "jdh:john_father/look_left";

Line("john_father", "Сядь.").SetAnimationFace(Angry);
Line("john", "Что случилось?");
Line("john_father", "Ты знаешь, что случилось.");
Line("john", "Нет.");
Line("john_father", "Тогда я напомню.");

StopFaceAnimation("john_father");

Line("john_father", "Посмотри на меня.").SetAnimationFace(CalmLook);
```

Главное не ставить на каждую строку одинаковый `SetAnimationFace(Angry)` без нужды. Если система каждый раз перезапускает клип, выражение может дергаться.

### Примеры диалогов (Dialogue): смешивание тела и лица

Если у персонажа есть body-анимация в профиле и лицевая facial-анимация в face-rig, их можно запускать рядом.

```csharp
var FatherAngry = "jdh:john_father/angry_brows";
var FatherPoint = "jdh:john_father/point_loop";

Line("john_father", "Вот туда.").SetAnimation(FatherPoint).SetAnimationFace(FatherAngry);
Line("john", "К двери?");
Line("john_father", "К двери.");

StopFaceAnimation("john_father");
StopAnimation("john_father");
```

У body и face разные ID-пространства:

```text
jdh:john_father/point_loop       -> body/story animation
jdh:john_father/angry_brows     -> facial animation
```

Одинаковые хвосты вроде `look_left` допустимы, но лучше именовать явно, чтобы сценарист не путал поворот тела и взгляд глазами.

### Примеры действий Story: play, wait, stop

Пример пайплайна действия Story для body-анимации. Это не лицевой overlay-слой, а полноценная анимация тела через `echo-story-animation-adapter`.

```csharp
Action("echo_animation:play_animation")
{
    Target("john_father");
    Animation("jdh:john_father/point_loop");
    Channel("gesture");
    BlendInTicks(6);
    Loop(true);
}

Action("echo_animation:wait_animation")
{
    Target("john_father");
    Channel("gesture");
    MinTicks(20);
}

Action("echo_animation:stop_animation")
{
    Target("john_father");
    Channel("gesture");
    BlendOutTicks(8);
}
```

Идея каналов:

- `base` - походка, стойка, крупная поза.
- `gesture` - жест рукой, кивок, поворот головы, который можно наложить поверх стойки.
- `face` обычно не надо использовать для body action, потому что лицом управляет отдельный лицевой стек.

### Заметки по телесному ригу (body rig): стандартные кости

В humanoid-риге чаще всего ожидаются такие body-кости:

```text
root
body
head
left_arm
left_forearm
right_arm
right_forearm
left_leg
left_shin
right_leg
right_shin
```

Для предметов важны сокеты. В runtime-ридере дефолтные сокеты устроены так:

```csharp
Socket("main_hand")
{
    ParentBone("right_forearm");
    Translation(0, -6, 0);
    Rotation(0, 0, 0);
}

Socket("off_hand")
{
    ParentBone("left_forearm");
    Translation(0, -6, 0);
    Rotation(0, 0, 0);
}
```

Сюжетный смысл:

- `main_hand` - правая рука, предметы, указание, жесты.
- `off_hand` - левая рука, второй предмет, поддерживающий жест.
- Если рука в body clip резко вращается, предмет в сокете тоже будет резко вращаться.
- Для катсцен лучше делать руки через отдельный жестовый канал `gesture`, чтобы не ломать базовую позу ног и корпуса.

### Телесный клип (Body clip): кивок головой

Файлы:

```text
data\jdh\echo_animation\animation_clips\chapter_1\family\nod_enter.animation-clip.es
data\jdh\echo_animation\animation_clips\chapter_1\family\nod_main.animation-clip.es
data\jdh\echo_animation\animation_clips\chapter_1\family\nod_exit.animation-clip.es
data\jdh\echo_animation\animations\chapter_1\family\nod_once.animation.es
```

Enter-клип:

```csharp
using mods.echo_animation.resources.animation;

class NodEnterClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip(
            "jdh:nod_enter",
            rig: "jdh:humanoid",
            durationTicks: 4,
            tracks:
            [
                Track("head")
                    .Frame(0).Rotation(0, 0, 0).Done()
                    .Frame(4).Rotation(18, 0, 0).Done(),

                Track("body")
                    .Frame(0).Rotation(0, 0, 0).Done()
                    .Frame(4).Rotation(2, 0, 0).Done()
            ]
        );
    }
}
```

Main-клип:

```csharp
using mods.echo_animation.resources.animation;

class NodMainClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip(
            "jdh:nod_main",
            rig: "jdh:humanoid",
            durationTicks: 8,
            loopSeam: true,
            tracks:
            [
                Track("head")
                    .Frame(0).Rotation(18, 0, 0).Done()
                    .Frame(4).Rotation(-6, 0, 0).Done()
                    .Frame(8).Rotation(18, 0, 0).Done(),

                Track("body")
                    .Frame(0).Rotation(2, 0, 0).Done()
                    .Frame(4).Rotation(-1, 0, 0).Done()
                    .Frame(8).Rotation(2, 0, 0).Done()
            ]
        );
    }
}
```

Exit-клип:

```csharp
using mods.echo_animation.resources.animation;

class NodExitClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip(
            "jdh:nod_exit",
            rig: "jdh:humanoid",
            durationTicks: 5,
            tracks:
            [
                Track("head")
                    .Frame(0).Rotation(18, 0, 0).Done()
                    .Frame(5).Rotation(0, 0, 0).Done(),

                Track("body")
                    .Frame(0).Rotation(2, 0, 0).Done()
                    .Frame(5).Rotation(0, 0, 0).Done()
            ]
        );
    }
}
```

Высокоуровневая анимация:

```csharp
using mods.echo_animation.resources.animation;

class NodOnceAnimation : AnimationResource
{
    public override AnimationDef Build()
    {
        return Animation("jdh:nod_once", rig: "jdh:humanoid")
        {
            Enter = "jdh:nod_enter",
            Main = "jdh:nod_main",
            Exit = "jdh:nod_exit",
            Loop = false,
            BlendInTicks = 2,
            BlendOutTicks = 4,
            Channel = "gesture"
        };
    }
}
```

### Телесный клип (Body clip): отрицание головой

```csharp
using mods.echo_animation.resources.animation;

class ShakeHeadMainClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip(
            "jdh:shake_head_main",
            rig: "jdh:humanoid",
            durationTicks: 14,
            loopSeam: true,
            tracks:
            [
                Track("head")
                    .Frame(0).Rotation(0, -16, 0).Done()
                    .Frame(4).Rotation(0, 16, 0).Done()
                    .Frame(9).Rotation(0, -12, 0).Done()
                    .Frame(14).Rotation(0, 0, 0).Done(),

                Track("body")
                    .Frame(0).Rotation(0, -2, 0).Done()
                    .Frame(4).Rotation(0, 2, 0).Done()
                    .Frame(14).Rotation(0, 0, 0).Done()
            ]
        );
    }
}
```

```csharp
using mods.echo_animation.resources.animation;

class ShakeHeadOnceAnimation : AnimationResource
{
    public override AnimationDef Build()
    {
        return Animation("jdh:shake_head_once", rig: "jdh:humanoid")
        {
            Main = "jdh:shake_head_main",
            Loop = false,
            BlendInTicks = 2,
            BlendOutTicks = 3,
            Channel = "gesture"
        };
    }
}
```

### Телесный клип (Body clip): указание правой рукой

Этот пример лучше запускать как loop и останавливать вручную в story/dialogue.

```csharp
using mods.echo_animation.resources.animation;

class RightHandPointLoopClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip(
            "jdh:right_hand_point_loop_clip",
            rig: "jdh:humanoid",
            durationTicks: 24,
            loopSeam: true,
            tracks:
            [
                Track("right_arm")
                    .Frame(0).Rotation(-55, -22, 6).Done()
                    .Frame(12).Rotation(-58, -20, 8).Done()
                    .Frame(24).Rotation(-55, -22, 6).Done(),

                Track("right_forearm")
                    .Frame(0).Rotation(-18, -8, 0).Done()
                    .Frame(12).Rotation(-20, -8, 0).Done()
                    .Frame(24).Rotation(-18, -8, 0).Done(),

                Track("left_arm")
                    .Frame(0).Rotation(4, 0, -3).Done()
                    .Frame(12).Rotation(5, 0, -4).Done()
                    .Frame(24).Rotation(4, 0, -3).Done(),

                Track("body")
                    .Frame(0).Rotation(0, -5, 0).Done()
                    .Frame(12).Rotation(0, -4, 0).Done()
                    .Frame(24).Rotation(0, -5, 0).Done()
            ]
        );
    }
}
```

```csharp
using mods.echo_animation.resources.animation;

class RightHandPointLoopAnimation : AnimationResource
{
    public override AnimationDef Build()
    {
        return Animation("jdh:john_father/point_loop", rig: "jdh:humanoid")
        {
            Main = "jdh:right_hand_point_loop_clip",
            Loop = true,
            BlendInTicks = 5,
            BlendOutTicks = 6,
            Channel = "gesture"
        };
    }
}
```

Использование в Story:

```csharp
Action("echo_animation:play_animation")
{
    Target("john_father");
    Animation("jdh:john_father/point_loop");
    Channel("gesture");
    Loop(true);
    BlendInTicks(5);
}

Line("john_father", "Иди туда.");
Line("john", "Сейчас?");
Line("john_father", "Сейчас.");

Action("echo_animation:stop_animation")
{
    Target("john_father");
    Channel("gesture");
    BlendOutTicks(6);
}
```

### Телесный клип (Body clip): быстрый поворот корпуса к игроку

```csharp
using mods.echo_animation.resources.animation;

class TurnToPlayerClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip(
            "jdh:turn_to_player_clip",
            rig: "jdh:humanoid",
            durationTicks: 10,
            tracks:
            [
                Track("body")
                    .Frame(0).Rotation(0, -18, 0).Done()
                    .Frame(6).Rotation(0, 4, 0).Done()
                    .Frame(10).Rotation(0, 0, 0).Done(),

                Track("head")
                    .Frame(0).Rotation(0, -12, 0).Done()
                    .Frame(6).Rotation(0, 2, 0).Done()
                    .Frame(10).Rotation(0, 0, 0).Done(),

                Track("right_arm")
                    .Frame(0).Rotation(8, 0, 2).Done()
                    .Frame(10).Rotation(0, 0, 0).Done(),

                Track("left_arm")
                    .Frame(0).Rotation(8, 0, -2).Done()
                    .Frame(10).Rotation(0, 0, 0).Done()
            ]
        );
    }
}
```

```csharp
using mods.echo_animation.resources.animation;

class TurnToPlayerAnimation : AnimationResource
{
    public override AnimationDef Build()
    {
        return Animation("jdh:turn_to_player", rig: "jdh:humanoid")
        {
            Main = "jdh:turn_to_player_clip",
            Loop = false,
            BlendInTicks = 1,
            BlendOutTicks = 3,
            Channel = "gesture"
        };
    }
}
```

### Профиль анимаций (Animation profile): привязка действий к профилю

Если body-анимация есть как ресурс, но NPC ее не проигрывает, проверьте profile и binding.

```csharp
using mods.echo_animation.resources.animation;

class StandardHumanoidProfile : AnimationProfileResource
{
    public override AnimationProfileDef Build()
    {
        return AnimationProfile("jdh:standard_humanoid")
        {
            Rig = "jdh:humanoid",

            Animations =
            [
                AnimationRef("idle", "jdh:humanoid_idle"),
                AnimationRef("walk", "jdh:humanoid_walk"),
                AnimationRef("wave", "jdh:wave"),
                AnimationRef("nod_once", "jdh:nod_once"),
                AnimationRef("shake_head_once", "jdh:shake_head_once"),
                AnimationRef("turn_to_player", "jdh:turn_to_player"),
                AnimationRef("point_loop", "jdh:john_father/point_loop")
            ]
        };
    }
}
```

Привязка NPC (NPC binding):

```csharp
using mods.echo_npc_animation.resources;

class NewCharacterAnimationBinding : NpcAnimationBindingResource
{
    public override NpcAnimationBindingDef Build()
    {
        return NpcAnimationBinding("jdh:new_character")
        {
            Profile = "jdh:standard_humanoid",
            Rig = "jdh:humanoid",
            FaceRig = "jdh:new_character",
            DefaultPose = "jdh:new_character/neutral"
        };
    }
}
```

Разница:

- `AnimationProfile` говорит, какие body-анимации доступны профилю.
- `NpcAnimationBinding` говорит, какой профиль, body rig, face rig и face pose получает конкретный NPC.
- Если `FaceRig` указан неправильно, лицевые facial-анимации могут существовать, но не применяться к нужному персонажу.
- Если `DefaultPose` указан на чужой ID, персонаж может появляться с неожиданным лицом.

### Полный чеклист персонажа с путями

Для нового персонажа `new_character` минимальный набор такой:

```text
content-pack\jdh\data\jdh\echo_animation\face_rigs\chapter_1\friends\new_character.face-rig.es
content-pack\jdh\data\jdh\echo_animation\face_poses\chapter_1\friends\new_character_neutral.face-pose.es
content-pack\jdh\data\jdh\echo_animation\face_animations\chapter_1\friends\new_character_look_left.face-animation.es
content-pack\jdh\data\jdh\echo_animation\face_animations\chapter_1\friends\new_character_look_right.face-animation.es
content-pack\jdh\data\jdh\echo_animation\face_animations\chapter_1\friends\new_character_angry_brows.face-animation.es
content-pack\jdh\data\jdh\echo_npc_animation\bindings\chapter_1\friends\new_character.npc-animation-binding.es
content-pack\jdh\assets\jdh\textures\echo_animation\faces\new_character\*.png
configs\jdh\echo_animation\portrait_overlays\new_character.portrait-overlay.es
```

Потом проверить:

```powershell
rg -n "jdh:new_character" $PrismPack $PrismConfig
rg -n "new_character" "$PrismPack\assets" "$PrismConfig"
```

Ожидаемые ID:

```text
jdh:new_character
jdh:new_character/neutral
jdh:new_character/look_left
jdh:new_character/look_right
jdh:new_character/angry_brows
```

### Исправления после копирования для текущего Prism-пака

Эти примеры показывают, как должны выглядеть подозрительные места, найденные в runtime-паке.

Нейтральная поза Molly:

```csharp
using mods.echo_animation.resources.face;

class MollyNeutralFacePose : FacePoseResource
{
    public override FacePoseDef Build()
    {
        return FacePose("jdh:molly/neutral", rig: "jdh:molly")
            .Layer("sclera_left").At(-0.125, 0.060, 0.000).Done()
            .Layer("sclera_right").At(0.125, 0.060, 0.000).Done()
            .Layer("pupil_left").At(-0.090, 0.000, 0.010).Done()
            .Layer("pupil_right").At(0.090, 0.000, 0.010).Done()
            .Layer("upper_eyelid_left").At(-0.125, -0.035, 0.020).Done()
            .Layer("upper_eyelid_right").At(0.125, -0.035, 0.020).Done()
            .Layer("lower_eyelid_left").At(-0.125, 0.153, 0.020).Done()
            .Layer("lower_eyelid_right").At(0.125, 0.153, 0.020).Done()
            .Layer("brows_left").At(-0.125, -0.032, 0.030).Done()
            .Layer("brows_right").At(0.124, -0.032, 0.030).Done()
            .Layer("glasses").At(0.000, 0.060, 0.040).Done();
    }
}
```

Нейтральная поза Sofa:

```csharp
using mods.echo_animation.resources.face;

class SofaNeutralFacePose : FacePoseResource
{
    public override FacePoseDef Build()
    {
        return FacePose("jdh:sofa/neutral", rig: "jdh:sofa")
            .Layer("sclera_left").At(-0.125, 0.060, 0.000).Done()
            .Layer("sclera_right").At(0.125, 0.060, 0.000).Done()
            .Layer("pupil_left").At(-0.090, 0.000, 0.010).Done()
            .Layer("pupil_right").At(0.090, 0.000, 0.010).Done()
            .Layer("upper_eyelid_left").At(-0.125, -0.035, 0.020).Done()
            .Layer("upper_eyelid_right").At(0.125, -0.035, 0.020).Done()
            .Layer("lower_eyelid_left").At(-0.125, 0.153, 0.020).Done()
            .Layer("lower_eyelid_right").At(0.125, 0.153, 0.020).Done()
            .Layer("brows_left").At(-0.125, -0.032, 0.030).Done()
            .Layer("brows_right").At(0.124, -0.032, 0.030).Done();
    }
}
```

Слой очков отца для портрета, если очки должны быть видны в портрете:

```csharp
Layer("glasses", placeX: "0", placeY: "0", scaleX: "1", scaleY: "1")
```

Ben/Natan без очков, если очки не нужны:

```csharp
// Удалить из ben.portrait-overlay.es и natan.portrait-overlay.es:
Layer("glasses", placeX: "0", placeY: "0.06", scaleX: "1", scaleY: "1")
```

Ben/Natan с очками, если очки нужны:

```csharp
Bone Glasses
{
    Position(0.000, 0.060, 0.040);
    Size(6, 2);
    Texture("glasses");
}
```

И добавить PNG:

```text
assets\jdh\textures\echo_animation\faces\ben\glasses.png
assets\jdh\textures\echo_animation\faces\natan\glasses.png
```

### Отладочные команды для примеров

Проверить, что новый face rig виден в content-pack:

```powershell
rg -n "face NewCharacter|jdh:new_character" $PrismPack -g "*.es"
```

Проверить, что у facial-анимаций не забыта surface `Portrait`:

```powershell
rg -n "FacialAnimation\\(|surfaces: \\[World\\]|surfaces: \\[World, Portrait\\]" $PrismPack -g "*.face-animation.es"
```

Найти facial-анимации, где подавляется idle-моргание:

```powershell
rg -n "suppressIdleBlink:\\s*true" $PrismPack -g "*.face-animation.es"
```

Найти body-анимации, которые loop:

```powershell
rg -n "Loop\\s*=\\s*true|loopSeam:\\s*true" $PrismPack -g "*.animation.es" -g "*.animation-clip.es"
```

Найти story/dialogue места, где анимация запускается, но может не останавливаться:

```powershell
rg -n "SetAnimationFace\\(|SetAnimation\\(|play_animation|StopFaceAnimation\\(|StopAnimation\\(|stop_animation" $PrismPack $PrismConfig "$JdhRepo\story"
```

### Правила именования

Для персонажа:

```text
jdh:ben
jdh:dinara
jdh:john_father
jdh:molly
jdh:natan
jdh:sasha
jdh:sofa
echo_content_player:player/base
```

Для нейтральной face pose:

```text
jdh:ben/neutral
jdh:dinara/neutral
jdh:john_father/neutral
jdh:molly/neutral
jdh:natan/neutral
jdh:sasha/neutral
jdh:sofa/neutral
echo_content_player:player/base/neutral
```

Для лицевых facial-анимаций:

```text
jdh:john_father/angry_brows
jdh:john_father/brows_raise
jdh:john_father/full_face_hold
jdh:john_father/look_left
jdh:molly/angry_brows
jdh:molly/brows_raise
jdh:molly/full_face_hold
jdh:molly/look_left
jdh:sasha/sleep
echo_content_player:player/brows_raise
echo_content_player:player/look_left
```

Рецепт для новых:

```text
jdh:<character>/<emotion_or_action>
jdh:<character>/look_left
jdh:<character>/look_right
jdh:<character>/angry_brows
jdh:<character>/sad
jdh:<character>/surprise
jdh:<character>/squint
jdh:<character>/sleep
```

### Правило авторинга: один бит сцены, одна визуальная причина

Не добавляйте анимацию просто потому, что строка есть. Каждая анимация должна отвечать на один вопрос:

- Что персонаж понял?
- Куда он смотрит?
- Что он скрывает?
- Кого он боится?
- Что он делает руками?
- Нужно ли зрителю заметить предмет?
- Нужно ли удержать взгляд игрока на портрете?

Пример плохого темпа:

```csharp
Line("john_father", "Я пришел.").SetAnimationFace("jdh:john_father/angry_brows");
Line("john_father", "Сядь.").SetAnimationFace("jdh:john_father/brows_raise");
Line("john_father", "Говори.").SetAnimationFace("jdh:john_father/look_left");
```

Почему плохо: три быстрые смены лица подряд выглядят как дергание, а не актерская игра.

Пример лучше:

```csharp
Line("john_father", "Я пришел.").SetAnimationFace("jdh:john_father/look_left");
Line("john_father", "Сядь.");
Line("john_father", "Говори.").SetAnimationFace("jdh:john_father/angry_brows");
StopFaceAnimation("john_father");
```

Почему лучше: сначала взгляд задает направление, потом злость ставится на смысловой удар.

## Модули, которые отвечают за анимации

`mods\echo-animation` - основной стек анимаций. Содержит контракты, вычисление сэмплов, валидацию, EchoScript-адаптеры, Forge-рендеринг и клиентские команды.

`mods\echo-story-animation-adapter` - мост Story -> Animation. Добавляет типы действий:

```text
echo_animation:play_animation
echo_animation:stop_animation
echo_animation:wait_animation
```

`mods\echo-dialogue` - диалоговый DSL. На уровне строк умеет читать:

```text
SetAnimation(...)
SetAnimationFace(...)
SetAnimationFaceClear()
StopFaceAnimation()
```

`mods\echo-npc-animation-adapter` - мост NPC -> Animation. Позволяет NPC быть `AnimationSubjectId`, принимать profile и проигрывать body/face позы.

`mods\echo-player-actor` и `mods\echo-player-animation-adapter` - аналогичная интеграция для игрока/John. В dialogue bridge есть особое сопоставление визуального ID игрока к `echo_content_player:player/base`.

`mods\echo-story-content-pack` - реальные story/content ресурсы проекта `jdh`. Большинство сюжетных правок должно идти сюда.

## ID и почему они ломают всё

В проекте есть два важных типа ID.

`BoneId` - локальный ID кости внутри rig-а. Он не namespaced. Примеры:

```text
head
right_forearm
face_root
left_pupil
brows_left
upper_eyelids
```

Правила `BoneId`:

- начинается с маленькой латинской буквы;
- дальше допускаются маленькие буквы, цифры, `_`, `/`, `-`;
- максимум 64 символа;
- не содержит namespace через `:`;
- должен существовать в rig-е, если используется в `Track(...)`, `BoneDelta(...)` или layer binding.

Namespaced ID - ID ресурсов с namespace. Примеры:

```text
jdh:john_father
jdh:john_father/angry_brows
jdh:wave
echo_content_player:player/base
```

Правила namespaced ID:

- формат `namespace:path`;
- namespace: `a-z`, `0-9`, `_`, `.`, `-`;
- path: `a-z`, `0-9`, `_`, `.`, `/`, `-`;
- не должно быть `//`;
- path не должен начинаться или заканчиваться `/`;
- всё приводится к нижнему регистру (`lowercase`) правилами парсера;
- максимум 128 символов.

Практическое правило для сюжетников: ID ресурса всегда namespaced, а ID кости, слоя или control почти всегда локальные.

Неправильно:

```csharp
Track("jdh:john_father/brows_left", frames: [...])
```

Правильно:

```csharp
Track("brows_left", frames: [...])
```

## Типы анимационных ресурсов

`AnimationRigResource` описывает body rig. В текущем проекте:

```text
data/jdh/echo_animation/animation_rigs/chapter_1/family/humanoid.animation-rig.es
```

```csharp
namespace JDH.Animation;

class HumanoidRig : AnimationRigResource
{
    public override AnimationRigDef Build()
    {
        return AnimationRig("jdh:humanoid", skin: "CLASSIC");
    }
}
```

`AnimationClipResource` описывает низкоуровневый body clip: набор треков по костям и нормализованных кадров. Примеры:

```text
animation_clips/chapter_1/family/wave/wave_enter.animation-clip.es
animation_clips/chapter_1/family/wave/wave_loop.animation-clip.es
animation_clips/chapter_1/family/wave/wave_exit.animation-clip.es
```

`AnimationResource` описывает высокоуровневый жизненный цикл: entry-фазу, main-фазу, exit-фазу, source/target-состояния, replay и recovery.

`AnimationProfileResource` говорит runtime, какой rig и graph использовать для субъекта, какие каналы обязательны и в каких состояниях они стартуют.

`NpcAnimationBindingResource` связывает NPC definition с профилем анимаций.

`FaceRigResource` описывает лицевой rig: лицевой скелет, слои лица и controls вроде blink.

`FacePoseResource` описывает статичную позу лица.

`FacialAnimationResource` описывает лицевую анимацию для 3D и/или портретной surface.

`PortraitOverlayConfig` подгоняет слои лица поверх текстуры портрета в Dialogue.

## Кости тела (Body bones)

Текущий body rig использует фиксированную A2 segmented humanoid hierarchy. Источник правил:

```text
mods/echo-animation/animation-contracts/src/main/java/io/github/kxyr0/echoanimation/api/rig/definition/SegmentedHumanoidBone.java
```

Все body-кости:

| BoneId | Родитель | Локальное смещение по умолчанию | За что отвечает |
|---|---|---:|---|
| `root` | нет | `(0, 0, 0)` | корень всего тела, глобальное основание скелета |
| `body` | `root` | `(0, 12, 0)` | корпус, плечи и голова наследуются от него |
| `head` | `body` | `(0, 12, 0)` | голова, повороты взгляда телом, кивки |
| `left_shoulder` | `body` | `(4, 10, 0)` | левое плечо, точка крепления руки |
| `left_upper_arm` | `left_shoulder` | `(0, 0, 0)` | левое плечо/верх руки |
| `left_forearm` | `left_upper_arm` | `(0, -6, 0)` | левое предплечье |
| `right_shoulder` | `body` | `(-4, 10, 0)` | правое плечо, точка крепления руки |
| `right_upper_arm` | `right_shoulder` | `(0, 0, 0)` | правое плечо/верх руки |
| `right_forearm` | `right_upper_arm` | `(0, -6, 0)` | правое предплечье |
| `left_thigh` | `root` | `(2, 0, 0)` | левое бедро |
| `left_shin` | `left_thigh` | `(0, -6, 0)` | левая голень |
| `right_thigh` | `root` | `(-2, 0, 0)` | правое бедро |
| `right_shin` | `right_thigh` | `(0, -6, 0)` | правая голень |

Самые частые body-треки:

- `head` - взгляд в сторону, кивок, отрицание, реакция на звук.
- `body` - наклон корпуса, дыхание, напряжение, разворот.
- `right_upper_arm`, `right_forearm` - волна рукой, указание, жест.
- `left_upper_arm`, `left_forearm` - вторичные жесты, баланс.
- `left_thigh`, `right_thigh`, `left_shin`, `right_shin` - ходьба, приседание, перенос веса.

Не используйте несуществующие vanilla names вроде `leftArm`, `right_leg`, `torso`, `neck`, если они не объявлены отдельным rig-ом. Для текущего `jdh:humanoid` правильные имена - только таблица выше.

## DSL телесного клипа (body clip)

Body clip - это набор треков по body-костям.

Типовой формат:

```csharp
namespace JDH.Animation;

class ExampleClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("jdh:example_clip",
            tracks: [
                Track("head", frames: [
                    Frame("0.0").Rotation(0, 0, 0),
                    Frame("0.5", interpolation: "SMOOTH").Rotation(0, 25, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ]),
                Track("right_forearm", frames: [
                    Frame("0.0").Rotation(0, 0, 0),
                    Frame("0.5").Rotation("-45.0", 0, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ])
            ]);
    }
}
```

В body clips текущего контент-пака кадры обычно нормализованы от `0.0` до `1.0`. Высокоуровневая `Animation(...)` затем задаёт, сколько тиков занимает clip в entry/main/exit-фазе.

Поддерживаемые модификаторы трансформации:

```csharp
Frame("0.5").Position(x, y, z)
Frame("0.5").Rotation(x, y, z)
Frame("0.5").Scale(x, y, z)
```

Интерполяция:

```text
STEP
LINEAR
SMOOTH
CUBIC
```

Практика:

- `LINEAR` - прямое движение, хорошо для простых механических фаз.
- `SMOOTH` - безопасный вариант для эмоций и естественных жестов.
- `CUBIC` - хорошо для ходьбы и органического движения, если start/end-позы совпадают.
- `STEP` - резкая смена позы, обычно для технических тестов или намеренного snap-перехода.

## Высокоуровневая телесная Animation(...)

`Animation(...)` - это не сам список ключевых кадров, а жизненный цикл вокруг клипов.

Текущий пример `jdh:wave`:

```csharp
namespace JDH.Animation;

class WaveAnimation : AnimationResource
{
    public override AnimationDef Build()
    {
        return Animation("jdh:wave",
            channels: [Channel("body")],
            sources: [Source("body/idle")],
            entries: [Entry("body/idle", clip: "jdh:wave_enter", durationTicks: 6)],
            main: Main(clip: "jdh:wave_loop", mode: "LOOP_UNTIL_STOP", durationTicks: 24,
                exitPoints: [ExitPoint("neutral", progress: "0.0")]),
            exits: [Exit("neutral", clip: "jdh:wave_exit", durationTicks: 6)],
            target: "body/idle",
            recovery: Recovery("BOUNDED", checkpointIntervalTicks: 8),
            replay: "REJECT_IF_RUNNING");
    }
}
```

Текущий пример `jdh:look_around_once`:

```csharp
namespace JDH.Animation;

class LookAroundOnceAnimation : AnimationResource
{
    public override AnimationDef Build()
    {
        return Animation("jdh:look_around_once",
            channels: [Channel("body")],
            sources: [Source("body/idle")],
            entries: [Entry("body/idle", clip: "jdh:look_around_enter", durationTicks: 4)],
            main: Main(clip: "jdh:look_around_main", mode: "ONCE", durationTicks: 48,
                exitPoints: [ExitPoint("complete", progress: "1.0")]),
            exits: [Exit("complete", clip: "jdh:look_around_exit", durationTicks: 4)],
            target: "body/idle",
            recovery: Recovery("BOUNDED", checkpointIntervalTicks: 6),
            replay: "REJECT_IF_RUNNING");
    }
}
```

Поля:

| Поле | Что делает |
|---|---|
| `channels` | какие animation channels затрагиваются, сейчас обычно `body` |
| `sources` | из каких graph states можно запускать animation |
| `entries` | как перейти из каждого source state в main clip |
| `main` | главный clip и режим проигрывания |
| `exitPoints` | допустимые точки выхода из main, нужны для loop-анимаций |
| `exits` | clip выхода для каждой точки выхода |
| `target` | состояние, куда вернётся subject после завершения |
| `recovery` | как восстанавливаться, если runtime нужно восстановить позу |
| `replay` | что делать, если animation уже запущена |

`MainMode`:

```text
ONCE
LOOP_UNTIL_STOP
HOLD_UNTIL_STOP
```

`ONCE` - main проигрывается один раз и завершает animation. Для действия Story можно использовать `await: "COMPLETED"`.

`LOOP_UNTIL_STOP` - main крутится, пока не придёт `stop_animation`. Для запуска из Story используйте `await: "STARTED"`, иначе сценарий может зависнуть или получить отказ.

`HOLD_UNTIL_STOP` - main удерживается до stop. Это подходит для позы ожидания, удержания предмета или freeze-like состояния.

`ReplayPolicy`:

```text
REJECT_IF_RUNNING
```

Если такая же animation уже запущена на target, повторный запуск отвергается.

`RecoveryPolicy`:

```text
EXACT
BOUNDED
```

`EXACT` требует точного восстановления. `BOUNDED` разрешает checkpoint-логику через `checkpointIntervalTicks`.

## Валидация телесной анимации (body animation)

Валидатор `AnimationDefinitionValidator` проверяет:

- `entries` должны ровно покрывать все `sources`;
- нельзя дважды объявить entry для одного source;
- `LOOP_UNTIL_STOP` обязан иметь хотя бы один `exitPoint`;
- нельзя дважды объявить один `exitPoint`;
- каждый `Exit(...)` должен ссылаться на существующий `ExitPoint`;
- каждый `ExitPoint` должен иметь ровно один `Exit(...)`;
- все ID клипов должны существовать;
- entry и exit-клипы должны покрывать все кости, которые есть в main clip;
- конец entry должен совпадать со стартом main;
- старт exit должен совпадать с позой main в точке выхода;
- конец exit должен вернуться в base pose rig-а;
- сравнение поз идёт с допуском `0.0001`.

Типовые ошибки:

`MISSING_ENTRY_BONE_COVERAGE` - main clip двигает кость, но entry clip не содержит track этой кости.

`MISSING_EXIT_BONE_COVERAGE` - main clip двигает кость, но exit clip не содержит track этой кости.

`ANIMATION_PHASE_POSE_MISMATCH` - entry/main или main/exit не совпали по позе.

`EXIT_TARGET_POSE_MISMATCH` - exit clip не вернулся в base pose.

Практическое правило: если main двигает `head`, `right_upper_arm`, `right_forearm`, то entry и exit тоже должны иметь tracks для всех трёх костей, даже если какие-то frames там нулевые.

## Текущие телесные body-анимации

| ID | Режим | Main-клип | Длительность main | Точка выхода | Назначение |
|---|---|---|---:|---|---|
| `jdh:wave` | `LOOP_UNTIL_STOP` | `jdh:wave_loop` | 24 тика | `neutral` в `0.0` | махание рукой до stop |
| `jdh:humanoid_walk` | `LOOP_UNTIL_STOP` | `jdh:humanoid_walk_loop` | из текущего ресурса | loop exit | ходьба humanoid |
| `jdh:look_around_once` | `ONCE` | `jdh:look_around_main` | 48 тиков | `complete` в `1.0` | одноразово оглянуться |
| `jdh:knee_bend` | `ONCE` | `jdh:knee_bend_main` | из текущего ресурса | complete-like exit | сгибание коленей/приседание |

Пути:

```text
data/jdh/echo_animation/animations/chapter_1/family/wave.animation.es
data/jdh/echo_animation/animations/chapter_1/family/humanoid_walk.animation.es
data/jdh/echo_animation/animations/chapter_1/family/look_around_once.animation.es
data/jdh/echo_animation/animations/chapter_1/family/knee_bend.animation.es
```

Клипы:

```text
data/jdh/echo_animation/animation_clips/chapter_1/family/wave/wave_enter.animation-clip.es
data/jdh/echo_animation/animation_clips/chapter_1/family/wave/wave_loop.animation-clip.es
data/jdh/echo_animation/animation_clips/chapter_1/family/wave/wave_exit.animation-clip.es

data/jdh/echo_animation/animation_clips/chapter_1/family/walk/humanoid_walk_enter.animation-clip.es
data/jdh/echo_animation/animation_clips/chapter_1/family/walk/humanoid_walk_loop.animation-clip.es
data/jdh/echo_animation/animation_clips/chapter_1/family/walk/humanoid_walk_exit.animation-clip.es

data/jdh/echo_animation/animation_clips/chapter_1/family/look_around/look_around_enter.animation-clip.es
data/jdh/echo_animation/animation_clips/chapter_1/family/look_around/look_around_main.animation-clip.es
data/jdh/echo_animation/animation_clips/chapter_1/family/look_around/look_around_exit.animation-clip.es

data/jdh/echo_animation/animation_clips/chapter_1/family/knee_bend/knee_bend_enter.animation-clip.es
data/jdh/echo_animation/animation_clips/chapter_1/family/knee_bend/knee_bend_main.animation-clip.es
data/jdh/echo_animation/animation_clips/chapter_1/family/knee_bend/knee_bend_exit.animation-clip.es
```

## Профиль анимаций (Animation profile)

Текущий profile:

```text
data/jdh/echo_animation/animation_profiles/chapter_1/family/standard_humanoid.animation-profile.es
```

```csharp
namespace JDH.Animation.Chapter1.Family;

class StandardHumanoidAnimationProfile : AnimationProfileResource
{
    public override AnimationProfileDef Build()
    {
        return AnimationProfile("jdh:standard_humanoid",
            rig: "jdh:humanoid",
            graph: "jdh:humanoid",
            render: "SEGMENTED_HUMANOID",
            initialStates: [
                InitialState("body", state: "body/idle")
            ],
            requiredChannels: [
                Channel("body")
            ],
            metadata: [
                Meta("authoring_note", value: "Default segmented humanoid profile for John Father.")
            ]
        );
    }
}
```

Поля:

- `rig` - body rig, который содержит кости.
- `graph` - namespace graph/state; в текущих ресурсах совпадает с `jdh:humanoid`.
- `render` - модель рендера, сейчас `SEGMENTED_HUMANOID`.
- `initialStates` - стартовое state каждого channel, например `body/idle`.
- `requiredChannels` - channels, которые должны быть доступны у subject.
- `metadata` - заметки и технические подписи.

## Привязка NPC к анимациям (NPC animation binding)

NPC получает профиль анимаций через binding:

```text
data/jdh/echo_npc_animation/bindings/chapter_1/family/john_father.npc-animation-binding.es
```

```csharp
namespace JDH.NpcAnimation.Chapter1.Family;

class JohnFatherAnimationBinding : NpcAnimationBindingResource
{
    public override NpcAnimationBindingDef Build()
    {
        return NpcAnimationBinding("jdh:john_father_standard_humanoid",
            npc: "jdh:john_father",
            profile: "jdh:standard_humanoid");
    }
}
```

Смысл:

- `npc` - ID определения NPC, например `jdh:john_father`;
- `profile` - ID профиля анимаций, например `jdh:standard_humanoid`;
- после этого `jdh:john_father` можно использовать как `target` в `echo_animation:play_animation`.

## Действия Story для body-анимаций

Реальный пример:

```text
data/jdh/echo_story/stories/chapter_1/home/home_animation_lifecycle.es
```

```csharp
Action(
    "play_look_once",
    type: "echo_animation:play_animation",
    animation: "jdh:look_around_once",
    target: "jdh:john_father",
    await: "COMPLETED",
    output: "look_result",
    next: "play_wave");

Action(
    "play_wave",
    type: "echo_animation:play_animation",
    animation: "jdh:wave",
    target: "jdh:john_father",
    await: "STARTED",
    output: "wave_start",
    next: "stop_wave");

Action(
    "stop_wave",
    type: "echo_animation:stop_animation",
    animation: "jdh:wave",
    target: "jdh:john_father",
    output: "wave_stop",
    next: "wait_wave_terminal");

Action(
    "wait_wave_terminal",
    type: "echo_animation:wait_animation",
    animation: "jdh:wave",
    target: "jdh:john_father",
    output: "wave_terminal",
    next: "finish");
```

`echo_animation:play_animation` поля:

| Поле | Обязательное | Значение |
|---|---:|---|
| `type` | да | `"echo_animation:play_animation"` |
| `animation` | да | обязательный ID высокоуровневой анимации, например `jdh:wave` |
| `target` | ?? | ID subject/actor, например `jdh:john_father` |
| `await` | нет | `STARTED` или `COMPLETED` |
| `output` | нет | имя story-переменной для результата |
| `next` | нет | следующий node |

`echo_animation:stop_animation` поля:

| Поле | Обязательное | Значение |
|---|---:|---|
| `type` | да | `"echo_animation:stop_animation"` |
| `animation` | да | ID анимации |
| `target` | ?? | ID subject/actor |
| `output` | нет | куда записать результат |
| `next` | нет | следующий node |

`echo_animation:wait_animation` поля:

| Поле | Обязательное | Значение |
|---|---:|---|
| `type` | да | `"echo_animation:wait_animation"` |
| `animation` | да | ID анимации |
| `target` | ?? | ID subject/actor |
| `output` | нет | куда записать итоговый результат |
| `next` | нет | следующий node |

`AnimationAwaitPolicy`:

```text
STARTED
COMPLETED
```

Ожидание (`await`) по умолчанию:

- `ONCE` -> `COMPLETED`;
- `LOOP_UNTIL_STOP` и `HOLD_UNTIL_STOP` -> `STARTED`.

Важное правило: `COMPLETED` отклоняется для анимаций не типа `ONCE`. Loop/hold должны запускаться с `STARTED`, а завершаться отдельным `stop_animation`.

Payload результата может содержать:

```text
animationId
animationSubjectId
commandId
outcome
phase
animationPlaybackId
diagnosticCode
subjectRevision
playbackRevision
message
```

Это полезно при отладке Story: если action не сработал, ищите `diagnosticCode` и `message` в выходной переменной.

## Лицевой риг (Face rig): что это

Face rig описывает, какие кости и визуальные слои есть у лица. Он нужен для:

- 3D-рендеринга лица на модели;
- портретного overlay-слоя в интерфейсе диалога;
- idle-моргания;
- треков facial-анимации;
- проверки, что animation не пытается двигать несуществующую кость.

Текущие продакшен face rigs:

```text
data/jdh/echo_animation/face_rigs/chapter_1/family/john_father.face-rig.es
data/jdh/echo_animation/face_rigs/chapter_1/player/player_base.face-rig.es
```

`jdh:john_father`:

```csharp
namespace JDH.Animation.Chapter1.Family;

face JohnFather("jdh:john_father")
{
    Eyes
    {
        Sclera Left  { Position(-0.125, 0, 0); }
        Sclera Right { Position( 0.125, 0, 0); }

        Pupil Left   { Position(-0.09, 0, 0.01); }
        Pupil Right  { Position( 0.09, 0, 0.01); }

        Eyelid Upper { Position(0, -0.035, 0.02); TextureArea(8, 13, 8, 1); }
        Eyelid Lower { Position(0, 0.09, 0.02); TextureArea(8, 12, 8, 1); }
    }

    Brows
    {
        Pivot(-62, 0);

        Left  { Position(-0.125, -0.032, 0.03); }
        Right { Position( 0.124, -0.032, 0.03); Pivot(62, 0); }
    }

    Bone Glasses;

    Blink "eye_closure"
    {
        Travel(35, -35);
        Gaze(1, 1);
        Idle(true);
        PeriodTicks(30);
        CloseTicks(2);
        HoldTicks(2);
        OpenTicks(2);
    }
}
```

`echo_content_player:player/base`:

```csharp
namespace JDH.Animation.Chapter1.Player;

face PlayerBase("echo_content_player:player/base")
{
    Textures("jdh:echo_animation/faces/player");

    Eyes
    {
        Sclera Left  { Position(-0.125, 0, 0); }
        Sclera Right { Position( 0.125, 0, 0); }

        Pupil Left   { Position(-0.09, 0, 0.01); }
        Pupil Right  { Position( 0.09, 0, 0.01); }

        Eyelid Upper { Position(0, -0.035, 0.02); TextureArea(8, 13, 8, 1); }
        Eyelid Lower { Position(0, 0.09, 0.02); TextureArea(8, 12, 8, 1); }
    }

    Brows
    {
        Pivot(-62, 0);

        Left  { Position(-0.125, -0.032, 0.03); }
        Right { Position( 0.124, -0.032, 0.03); Pivot(62, 0); }
    }

    Blink "eye_closure"
    {
        Travel(35, -35);
        Gaze(1, 1);
        Idle(true);
        PeriodTicks(30);
        CloseTicks(2);
        HoldTicks(2);
        OpenTicks(2);
    }
}
```

## Сгенерированные кости лица (face bones)

High-level `face ... { Eyes { ... } Brows { ... } Bone Glasses; }` lowering автоматически создаёт кости. Это важно: в треках facial-анимации нужно использовать сгенерированные BoneId, а не названия DSL-секций.

Основная иерархия:

| BoneId | Родитель | Откуда появляется | За что отвечает |
|---|---|---|---|
| `face_root` | нет | любой `face ...` | корень всех лицевых элементов |
| `left_eye_root` | `face_root` | `Sclera Left` или `Pupil Left` | группа левого глаза |
| `right_eye_root` | `face_root` | `Sclera Right` или `Pupil Right` | группа правого глаза |
| `left_sclera` | `left_eye_root` | `Sclera Left` | белок левого глаза |
| `right_sclera` | `right_eye_root` | `Sclera Right` | белок правого глаза |
| `left_pupil` | `left_sclera` | `Pupil Left` | левый зрачок, основная кость для движения взгляда |
| `right_pupil` | `right_sclera` | `Pupil Right` | правый зрачок |
| `upper_eyelids` | `face_root` | `Eyelid Upper` | общая кость верхних век, обычно двигается blink/эмоциями |
| `upper_eyelid_left` | `upper_eyelids` | разделенный слой верхнего века | левый кусок верхнего века |
| `upper_eyelid_right` | `upper_eyelids` | разделенный слой верхнего века | правый кусок верхнего века |
| `lower_eyelids` | `face_root` | `Eyelid Lower` | общая кость нижних век |
| `lower_eyelid_left` | `lower_eyelids` | разделенный слой нижнего века | левый кусок нижнего века |
| `lower_eyelid_right` | `lower_eyelids` | разделенный слой нижнего века | правый кусок нижнего века |
| `brows_left` | `face_root` | `Brows { Left ... }` | левая бровь, текущая правильная кость для анимаций |
| `brows_right` | `face_root` | `Brows { Right ... }` | правая бровь |
| `glasses` | `face_root` | `Bone Glasses;` | очки и слой `glasses`, если нужен |

Запомнить:

- кости бровей сейчас называются `brows_left` и `brows_right`;
- слои бровей называются `brow_left` и `brow_right`;
- кость века для общей эмоции - `upper_eyelids` или `lower_eyelids`;
- разделенные bones/layers век - `upper_eyelid_left`, `upper_eyelid_right`, `lower_eyelid_left`, `lower_eyelid_right`;
- зрачки двигаются через кости `left_pupil` и `right_pupil`;
- `face_root` можно двигать для лёгкого общего сдвига лица.

## Сгенерированные слои лица (face layers)

Face layer - это визуальный quad/texture, привязанный к bone. Layer ID и BoneId не всегда совпадают.

| Часть DSL | LayerId | BoneId | Роль | Порядок отрисовки | Атлас по умолчанию |
|---|---|---|---|---:|---|
| `Sclera Left` | `sclera_left` | `left_sclera` | `DECORATION` | 10 | `2x2` |
| `Sclera Right` | `sclera_right` | `right_sclera` | `DECORATION` | 11 | `2x2` |
| `Pupil Left` | `pupil_left` | `left_pupil` | `PUPIL` | 15 | `1x2` |
| `Pupil Right` | `pupil_right` | `right_pupil` | `PUPIL` | 16 | `1x2` |
| `Eyelid Upper` левая половина | `upper_eyelid_left` | `upper_eyelid_left` | `EYELID` | 20 | половина авторского прямоугольника |
| `Eyelid Upper` правая половина | `upper_eyelid_right` | `upper_eyelid_right` | `EYELID` | 21 | половина авторского прямоугольника |
| `Eyelid Lower` левая половина | `lower_eyelid_left` | `lower_eyelid_left` | `EYELID` | 22 | половина авторского прямоугольника |
| `Eyelid Lower` правая половина | `lower_eyelid_right` | `lower_eyelid_right` | `EYELID` | 23 | половина авторского прямоугольника |
| `Brows Left` | `brow_left` | `brows_left` | `BROW` | 30 | `2x1` |
| `Brows Right` | `brow_right` | `brows_right` | `BROW` | 31 | `2x1` |
| `Bone Glasses` | `glasses` | `glasses` | `GLASSES` | 40 | `8x8` |

Роли слоев:

```text
SKIN
PUPIL
EYELID
BROW
GLASSES
DECORATION
```

Sclera high-level convention использует `DECORATION`, а не отдельный `SCLERA`, потому что enum `LayerRole` не содержит `SCLERA`.

Порядок отрисовки определяет порядок отрисовки. Чем больше order, тем выше слой. Если два слоя с одинаковым `drawOrder` пересекаются по границам, валидатор отклонит rig.

## Правила текстур для лицевого рига (face rig)

Если `Textures(...)` не указан, корень текстур по умолчанию строится из face rig ID:

```text
jdh:john_father -> jdh:echo_animation/faces/john_father
```

Для `jdh:john_father` текстуры по умолчанию:

```text
jdh:echo_animation/faces/john_father/sclera_left
jdh:echo_animation/faces/john_father/sclera_right
jdh:echo_animation/faces/john_father/pupil_left
jdh:echo_animation/faces/john_father/pupil_right
jdh:echo_animation/faces/john_father/upper_eyelids
jdh:echo_animation/faces/john_father/lower_eyelids
jdh:echo_animation/faces/john_father/brow_left
jdh:echo_animation/faces/john_father/brow_right
jdh:echo_animation/faces/john_father/glasses
```

Для player rig указан override:

```csharp
Textures("jdh:echo_animation/faces/player");
```

Поэтому `Pupil Left` у player ищет:

```text
jdh:echo_animation/faces/player/pupil_left
```

Физические PNG в content-pack:

```text
assets/jdh/textures/echo_animation/faces/john_father/brow_left.png
assets/jdh/textures/echo_animation/faces/john_father/brow_right.png
assets/jdh/textures/echo_animation/faces/john_father/glasses.png
assets/jdh/textures/echo_animation/faces/john_father/lower_eyelids.png
assets/jdh/textures/echo_animation/faces/john_father/pupil_left.png
assets/jdh/textures/echo_animation/faces/john_father/pupil_right.png
assets/jdh/textures/echo_animation/faces/john_father/sclera_left.png
assets/jdh/textures/echo_animation/faces/john_father/sclera_right.png
assets/jdh/textures/echo_animation/faces/john_father/upper_eyelids.png

assets/jdh/textures/echo_animation/faces/player/brow_left.png
assets/jdh/textures/echo_animation/faces/player/brow_right.png
assets/jdh/textures/echo_animation/faces/player/lower_eyelids.png
assets/jdh/textures/echo_animation/faces/player/pupil_left.png
assets/jdh/textures/echo_animation/faces/player/pupil_right.png
assets/jdh/textures/echo_animation/faces/player/sclera_left.png
assets/jdh/textures/echo_animation/faces/player/sclera_right.png
assets/jdh/textures/echo_animation/faces/player/upper_eyelids.png
```

## Единицы измерения в лицевом риге и лицевой анимации

Есть два authoring стиля чисел:

1. Малые десятичные числа уже воспринимаются как реальные локальные единицы:

```csharp
Position(-0.125, -0.032, 0.03)
Frame(6).Position("-0.031", 0, 0)
```

2. Большие целые значения в `Travel`, `Gaze`, `Pivot` проходят через milli-конвертацию:

```csharp
Travel(35, -35)  -> 0.035 and -0.035
Pivot(-62, 0)    -> -0.062 and 0
```

В facial animations для лица rotation поддерживает только Z:

```csharp
Frame(6).Rotation(0, 0, "-4.0")
Frame(6).Rotation("-4.0", 0, 0) // ошибка для face animation
```

Для body clips X/Y/Z rotation допустимы, потому что это пространство body-анимации.

## Управление морганием (Blink controls)

Blink control создаётся в face rig:

```csharp
Blink "eye_closure"
{
    Travel(35, -35);
    Gaze(1, 1);
    Idle(true);
    PeriodTicks(30);
    CloseTicks(2);
    HoldTicks(2);
    OpenTicks(2);
}
```

Смысл:

- `eye_closure` - `FaceControlId`;
- `Travel(upper, lower)` - насколько верхние/нижние веки едут при закрытии;
- `Gaze(x, y)` - диапазон движения gaze/control;
- `Idle(true)` - runtime может сам моргать этим control;
- `PeriodTicks` - как часто idle-моргание срабатывает;
- `CloseTicks` - фаза закрытия;
- `HoldTicks` - сколько держать закрытым;
- `OpenTicks` - фаза открытия.

High-level lowerer привязывает blink к костям:

```text
upper_eyelids
lower_eyelids
```

Если `Idle(true)`, валидатор требует, чтобы эти кости существовали. Поэтому нельзя добавить Blink без `Eyelid Upper` и `Eyelid Lower`.

Facial animation может подавить idle-моргание:

```csharp
return FacialAnimation("jdh:example/tense",
    rig: "jdh:john_father",
    surfaces: [World, Portrait],
    suppressIdleBlink: true);
```

Это полезно для удерживаемых эмоций, где автоматическое моргание ломает выражение.

## Валидация лицевого рига (face rig)

`FaceRigValidator` проверяет:

- каждый `Layer(...)` ссылается на существующую кость;
- нет повторяющихся `FaceLayerId`;
- `UVRect`, если задан, помещается в `64x64`;
- канонических родителей для известных костей, если они присутствуют;
- pupil layer требует соответствующий слой sclera;
- `PUPIL` не может быть единственным глазным слоем без sclera;
- layers с одинаковым `drawOrder` не должны пересекаться;
- idle-моргание требует верхние и нижние eyelid bones.

Проверки канонических родителей:

```text
left_eye_root -> face_root
right_eye_root -> face_root
left_sclera -> left_eye_root
right_sclera -> right_eye_root
left_pupil -> left_sclera
right_pupil -> right_sclera
left_brow -> face_root       // legacy/manual name, проверяется только если есть
right_brow -> face_root      // legacy/manual name, проверяется только если есть
glasses_root -> face_root    // legacy/manual name, проверяется только если есть
```

В текущем high-level DSL брови создаются как `brows_left`/`brows_right`, а glasses bone как `glasses`. Это нормально.

## Статичная поза лица (FacePoseResource)

Face pose - статичная поза лица. Текущий production-пример:

```text
data/jdh/echo_animation/face_poses/chapter_1/family/john_father_neutral.face-pose.es
```

Смысл такой:

```csharp
return FacePose("jdh:john_father/neutral",
    rig: "jdh:john_father",
    bones: [
        BoneDelta("face_root")
    ],
    controls: [
        ControlDelta("eye_closure", value: 0)
    ]);
```

`BoneDelta("face_root")` без transform означает нейтральный delta. `ControlDelta("eye_closure", value: 0)` явно открывает состояние blink/eye closure.

Face pose полезен, когда нужна базовая эмоция/состояние без timeline.

## Лицевая анимация (FacialAnimationResource)

Facial animation - это timeline по face bones. Она отличается от body `Animation(...)`: у неё есть surfaces `3d` и `portrait`, фазы `Enter`, `Stay`, `Exit`, и она запускается из Dialogue через `SetAnimationFace(...)`.

Поддерживаемые поля:

```text
rig
durationMillis
durationTicks
end
tracks
surfaces
suppressIdleBlink
```

Нельзя одновременно использовать `durationMillis` и `durationTicks`.

`AnimationEndBehavior`:

```text
HOLD
RETURN_TO_BASE
LOOP
```

`HOLD` - после последнего кадра держит последнюю позу фазы.  
`RETURN_TO_BASE` - возвращается к base pose.  
`LOOP` - зацикливает фазу.

Имена surface-классов:

```csharp
class World : FacialAnimationSurface3d
class Portrait : FacialAnimationSurfacePortrait
```

Они превращаются в runtime-имена surface:

```text
3d
portrait
```

Типовой production-паттерн:

```csharp
namespace JDH.Animation.Chapter1.Family;

class JohnFatherBrowsRaiseAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation("jdh:john_father/brows_raise",
            rig: "jdh:john_father",
            surfaces: [World, Portrait]);
    }
}

class World : FacialAnimationSurface3d
{
    private void Enter()
    {
        Phase(durationTicks: 5, end: "HOLD", tracks: [
            Track("brows_left", frames: [
                Frame(0).Position(0, 0, 0),
                Frame(5).Position(0, "-0.018", 0)
            ]),
            Track("brows_right", frames: [
                Frame(0).Position(0, 0, 0),
                Frame(5).Position(0, "-0.018", 0)
            ])
        ]);
    }

    private void Stay()
    {
        Phase(durationTicks: 12, end: "HOLD", tracks: [
            Track("brows_left", frames: [
                Frame(0).Position(0, "-0.018", 0),
                Frame(12).Position(0, "-0.018", 0)
            ]),
            Track("brows_right", frames: [
                Frame(0).Position(0, "-0.018", 0),
                Frame(12).Position(0, "-0.018", 0)
            ])
        ]);
    }

    private void Exit()
    {
        Phase(durationTicks: 7, end: "RETURN_TO_BASE", tracks: [
            Track("brows_left", frames: [
                Frame(0).Position(0, "-0.018", 0),
                Frame(7).Position(0, 0, 0)
            ]),
            Track("brows_right", frames: [
                Frame(0).Position(0, "-0.018", 0),
                Frame(7).Position(0, 0, 0)
            ])
        ]);
    }
}

class Portrait : FacialAnimationSurfacePortrait
{
    private void Enter()
    {
        Phase(durationTicks: 5, end: "HOLD", tracks: [
            Track("brows_left", frames: [
                Frame(0).Position(0, 0, 0),
                Frame(5).Position(0, "-0.010", 0)
            ]),
            Track("brows_right", frames: [
                Frame(0).Position(0, 0, 0),
                Frame(5).Position(0, "-0.010", 0)
            ])
        ]);
    }

    private void Stay()
    {
        Phase(durationTicks: 12, end: "HOLD", tracks: [
            Track("brows_left", frames: [
                Frame(0).Position(0, "-0.010", 0),
                Frame(12).Position(0, "-0.010", 0)
            ]),
            Track("brows_right", frames: [
                Frame(0).Position(0, "-0.010", 0),
                Frame(12).Position(0, "-0.010", 0)
            ])
        ]);
    }

    private void Exit()
    {
        Phase(durationTicks: 7, end: "RETURN_TO_BASE", tracks: [
            Track("brows_left", frames: [
                Frame(0).Position(0, "-0.010", 0),
                Frame(7).Position(0, 0, 0)
            ]),
            Track("brows_right", frames: [
                Frame(0).Position(0, "-0.010", 0),
                Frame(7).Position(0, 0, 0)
            ])
        ]);
    }
}
```

Почему World и Portrait часто отличаются:

- 3D-единицы лица и портретные overlay-единицы визуально ощущаются по-разному;
- портрет может требовать меньшего смещения бровей/век;
- в 3D допустимы более выраженные микродвижения;
- портретный overlay-слой должен совпасть с конкретным PNG-портретом.

## Правила кадров лицевой анимации (facial animation)

Для `FacialAnimation(... durationTicks: N)`:

```csharp
Frame(0)
Frame(5)
Frame(12)
```

означают тики внутри phase.

Для `durationMillis` время кадра читается как миллисекунды.

Модификаторы трансформации:

```csharp
Frame(0).Position(0, 0, 0)
Frame(6).Position("-0.031", 0, 0)
Frame(6).Rotation(0, 0, "-4.0")
Frame(6).Scale("1.0", "1.0", "1.0")
```

Вращение лица:

- поддерживает только Z;
- можно писать `Rotation(-4.0)` как один аргумент для Z;
- можно писать `Rotation(0, 0, "-4.0")`;
- X/Y во вращении лица вызовут ошибку.

Валидация Track:

- каждый `Track("bone")` должен ссылаться на bone из face rig;
- дублирующиеся tracks в одной animation отклоняются;
- если `rig` указан, animation подходит только к этому rig;
- если `rig` не указан, animation считается универсальной, но всё равно сработает только на rig-е, где есть нужные кости.

## Текущие лицевые facial-анимации

Production/shared:

| ID | Риг | Кости | Назначение |
|---|---|---|---|
| `jdh:shared/blink` | нет/универсальная | `upper_eyelids`, `lower_eyelids` | ручное/общее моргание |
| `jdh:john_father/look_left` | `jdh:john_father` | `left_pupil`, `right_pupil` | отец смотрит влево |
| `jdh:john_father/brows_raise` | `jdh:john_father` | `brows_left`, `brows_right` | вопрос, удивление, внимание |
| `jdh:john_father/angry_brows` | `jdh:john_father` | `brows_left`, `brows_right` | злость, подозрение, давление |
| `jdh:john_father/full_face_hold` | `jdh:john_father` | brows, eyelids, pupils | напряжённая удерживаемая эмоция |
| `jdh:player/look_left` | `echo_content_player:player/base` | pupils, eyelids, brows | John/player смотрит влево |
| `jdh:player/brows_raise` | `echo_content_player:player/base` | `brows_left`, `brows_right` | John/player поднимает брови |

Тестовые facial-анимации:

| ID | Назначение |
|---|---|
| `jdh:face_test/look_left` | универсальный тест взгляда влево |
| `jdh:face_test/full_face_hold` | полный тест удержания всего лица |
| `jdh:face_test/face_nudge` | двигает `face_root` |
| `jdh:face_test/brows_raise` | тест поднятия бровей |
| `jdh:face_test/blink` | тест blink |
| `jdh:face_test/angry_brows` | тест злых бровей |

Пути:

```text
data/jdh/echo_animation/face_animations/shared/blink.face-animation.es

data/jdh/echo_animation/face_animations/chapter_1/family/john_father/look_left.face-animation.es
data/jdh/echo_animation/face_animations/chapter_1/family/john_father/brows_raise.face-animation.es
data/jdh/echo_animation/face_animations/chapter_1/family/john_father/angry_brows.face-animation.es
data/jdh/echo_animation/face_animations/chapter_1/family/john_father/full_face_hold.face-animation.es

data/jdh/echo_animation/face_animations/chapter_1/player/look_left.face-animation.es
data/jdh/echo_animation/face_animations/chapter_1/player/brows_raise.face-animation.es

data/jdh/echo_animation/face_animations/tests/face_test_look_left.face-animation.es
data/jdh/echo_animation/face_animations/tests/face_test_brows_raise.face-animation.es
data/jdh/echo_animation/face_animations/tests/face_test_angry_brows.face-animation.es
data/jdh/echo_animation/face_animations/tests/face_test_blink.face-animation.es
data/jdh/echo_animation/face_animations/tests/face_test_face_nudge.face-animation.es
data/jdh/echo_animation/face_animations/tests/face_test_full_face_hold.face-animation.es
```

## Как писать эмоции

Поднять брови:

```csharp
Track("brows_left", frames: [
    Frame(0).Position(0, 0, 0),
    Frame(5).Position(0, "-0.018", 0)
]),
Track("brows_right", frames: [
    Frame(0).Position(0, 0, 0),
    Frame(5).Position(0, "-0.018", 0)
])
```

Свести брови в сердитое выражение:

```csharp
Track("brows_left", frames: [
    Frame(0).Position(0, 0, 0).Rotation(0, 0, 0),
    Frame(6).Position("0.004", "-0.006", 0).Rotation(0, 0, "-4.0")
]),
Track("brows_right", frames: [
    Frame(0).Position(0, 0, 0).Rotation(0, 0, 0),
    Frame(6).Position("-0.004", "-0.006", 0).Rotation(0, 0, "4.0")
])
```

Взгляд влево:

```csharp
Track("left_pupil", frames: [
    Frame(0).Position(0, 0, 0),
    Frame(6).Position("-0.031", 0, 0)
]),
Track("right_pupil", frames: [
    Frame(0).Position(0, 0, 0),
    Frame(6).Position("-0.031", 0, 0)
])
```

Полуприщур:

```csharp
Track("upper_eyelids", frames: [
    Frame(0).Position(0, 0, 0),
    Frame(6).Position(0, "0.014", 0)
]),
Track("lower_eyelids", frames: [
    Frame(0).Position(0, 0, 0),
    Frame(6).Position(0, "-0.010", 0)
])
```

Общий сдвиг лица:

```csharp
Track("face_root", frames: [
    Frame(0).Position(0, 0, 0),
    Frame(5).Position("0.012", "0.008", 0),
    Frame(10).Position(0, 0, 0)
])
```

Моргание:

```csharp
Track("upper_eyelids", frames: [
    Frame(0).Position(0, 0, 0),
    Frame(3).Position(0, "0.035", 0)
]),
Track("lower_eyelids", frames: [
    Frame(0).Position(0, 0, 0),
    Frame(3).Position(0, "-0.035", 0)
])
```

## Портретный overlay (PortraitOverlay)

Portrait overlay нужен, чтобы те же face layers легли поверх dialogue portrait PNG.

Текущие конфиги:

```text
story/configs/jdh/echo_animation/portrait_overlays/john_father.portrait-overlay.es
story/configs/jdh/echo_animation/portrait_overlays/player_base.portrait-overlay.es
```

`jdh:john_father`:

```csharp
namespace JDH.Configs.EchoAnimation;

class JohnFatherPortraitOverlay : PortraitOverlayConfig
{
    public override PortraitOverlayDef Build()
    {
        return PortraitOverlay("jdh:john_father",
            centerX: "0.50",
            centerY: "0.49",
            offsetX: 0,
            offsetY: 0,
            facePixels: "4",
            layers: [
                Layer("sclera_left", placeX: "-0.062", placeY: "0.002", scaleX: "0.50", scaleY: "0.50"),
                Layer("sclera_right", placeX: "0.062", placeY: "0.002", scaleX: "0.50", scaleY: "0.50"),
                Layer("pupil_left", placeX: "-0.062", placeY: "0.002", scaleX: "0.50", scaleY: "0.50"),
                Layer("pupil_right", placeX: "0.0309", placeY: "0.002", scaleX: "0.50", scaleY: "0.50"),
                Layer("upper_eyelid_left", placeX: "-0.062", placeY: "-0.034", scaleX: "0.50", scaleY: "0.50"),
                Layer("upper_eyelid_right", placeX: "0.062", placeY: "-0.034", scaleX: "0.50", scaleY: "0.50"),
                Layer("lower_eyelid_left", placeX: "-0.062", placeY: "0.034", scaleX: "0.50", scaleY: "0.50"),
                Layer("lower_eyelid_right", placeX: "0.062", placeY: "0.034", scaleX: "0.50", scaleY: "0.50"),
                Layer("brow_left", placeX: "-0.062", placeY: "-0.029", scaleX: "0.50", scaleY: "0.50"),
                Layer("brow_right", placeX: "0.062", placeY: "-0.029", scaleX: "0.50", scaleY: "0.50")
            ],
            idle: [
                Blink "eye_closure"
                {
                    Travel(20, -20);
                    Gaze(1, 1);
                    Idle(true);
                    PeriodTicks(50);
                    CloseTicks(2);
                    HoldTicks(2);
                    OpenTicks(2);
                }
            ]
        );
    }
}
```

`echo_content_player:player/base`:

```csharp
namespace JDH.Configs.EchoAnimation;

class PlayerBasePortraitOverlay : PortraitOverlayConfig
{
    public override PortraitOverlayDef Build()
    {
        return PortraitOverlay("echo_content_player:player/base",
            centerX: "0.5",
            centerY: "0.49",
            offsetX: 0,
            offsetY: 0,
            facePixels: "8",
            layers: [
                Layer("sclera_left", placeX: "-0.125", placeY: "0.0637", scaleX: "1", scaleY: "1"),
                Layer("sclera_right", placeX: "0.125", placeY: "0.0637", scaleX: "1", scaleY: "1"),
                Layer("pupil_left", placeX: "-0.1", placeY: "0.063", scaleX: "1", scaleY: "1"),
                Layer("pupil_right", placeX: "0.089", placeY: "0.063", scaleX: "1", scaleY: "1"),
                Layer("upper_eyelid_left", placeX: "-0.125", placeY: "0", scaleX: "1", scaleY: "1"),
                Layer("upper_eyelid_right", placeX: "0.125", placeY: "0", scaleX: "1", scaleY: "1"),
                Layer("lower_eyelid_left", placeX: "-0.125", placeY: "0.125", scaleX: "1", scaleY: "1"),
                Layer("lower_eyelid_right", placeX: "0.125", placeY: "0.125", scaleX: "1", scaleY: "1"),
                Layer("brow_left", placeX: "-0.125", placeY: "0.001", scaleX: "1", scaleY: "1"),
                Layer("brow_right", placeX: "0.125", placeY: "0.001", scaleX: "1", scaleY: "1")
            ],
            idle: [
                Blink "eye_closure"
                {
                    Travel(32, -32);
                    Gaze(1, 1);
                    Idle(true);
                    PeriodTicks(50);
                    CloseTicks(2);
                    HoldTicks(2);
                    OpenTicks(2);
                }
            ]
        );
    }
}
```

Что за что отвечает:

- `PortraitOverlay("id")` должен совпадать с visual ID портрета и face rig ID.
- `centerX`, `centerY` - центр лица на изображении портрета.
- `offsetX`, `offsetY` - общий сдвиг overlay.
- `facePixels` - масштаб системы координат лица относительно пикселей портрета.
- `Layer("layer_id", ...)` - размещение конкретного face layer на портрете.
- `idle` - отдельные idle controls для портретной surface. Они могут отличаться от 3D rig.

Если facial animation видна в 3D, но не видна в портрете, почти всегда проблема в одном из трёх мест:

- в `FacialAnimation` нет `Portrait` surface;
- `Alias(... visual: Portrait("..."))` указывает не на тот visual ID;
- `PortraitOverlay("...")` отсутствует или размещения слоев не совпадают с ID слоев.

## Пайплайн лицевой анимации в Dialogue

`DialogueResource` превращает цепочки методов в события cues:

```text
.SetAnimationFace(id)    -> faceAnimation=<id>
.SetAnimationFaceClear() -> faceAnimationClear=true
.StopFaceAnimation()     -> faceAnimationStop=true
.SetAnimation(id)        -> bodyAnimation=<id>
```

На одной реплике нельзя конфликтовать face-сеттеры:

```csharp
Line("father", "Text").SetAnimationFace(FatherAngry).SetAnimationFaceClear(); // ошибка
Line("father", "Text").SetAnimationFace(FatherAngry).StopFaceAnimation();     // ошибка
```

Правильно:

```csharp
Line("father", "Ты поздно вернулся.").SetAnimationFace(FatherAngry);
Line("father", "Я ждал почти весь вечер.").StopFaceAnimation();
Line("john", "Я знаю.").SetAnimationFace(PlayerLookLeft);
Line("john", "Я могу объяснить.").SetAnimationFaceClear();
```

`SetAnimationFace(...)` ожидает `FaceAnimationId`. Если передать field типа `BodyAnimationId`, compiler выдаст `DIALOGUE_WRONG_ANIMATION_ID_TYPE`.

`StopFaceAnimation()`:

- не принимает аргументов;
- переводит активную facial animation в exit-фазу;
- если exit-фаза отсутствует или уже закончилась, animation убирается;
- подходит для естественного выхода из эмоции.

`SetAnimationFaceClear()`:

- не принимает аргументов;
- чистит активное состояние лицевой анимации сразу;
- не проигрывает exit;
- подходит для резкой смены speaker/visual, смены сцены или принудительного сброса.

## Как Dialogue выбирает риг для лицевой анимации

В `DialogueVisualFaceLayerBridge` face cue применяется сразу к нескольким ключам:

```text
visualId
speakerId
base player face rig, если visual/speaker относится к John/player
```

Для NPC обычно должно совпадать:

```text
Alias speaker visual: jdh:john_father
FaceRig ID:           jdh:john_father
FacialAnimation rig:  jdh:john_father
PortraitOverlay ID:   jdh:john_father
NPC actor/definition: jdh:john_father
```

Для player/John:

```text
FaceRig ID:          echo_content_player:player/base
PortraitOverlay ID:  echo_content_player:player/base
Player animations:   jdh:player/look_left, jdh:player/brows_raise
Bridge base mapping: echo_content_player:player/base
```

Если `rig` в `FacialAnimation(...)` указан, runtime отфильтрует animation по rig ID. Animation не применится к другому rig-у даже если там есть такие же кости.

Если `rig` не указан, animation универсальная. Пример: `jdh:shared/blink`.

## Реальный Dialogue с лицевыми cues

Текущий dialogue:

```text
data/jdh/echo_dialogue/dialogues/chapter_1/home/home_arrival.dialogue.es
```

В нём объявлены face ID:

```csharp
private FaceAnimationId PlayerLookLeft = "jdh:player/look_left";
private FaceAnimationId PlayerBrowsRaise = "jdh:player/brows_raise";
private FaceAnimationId FatherAngryBrows = "jdh:john_father/angry_brows";
private FaceAnimationId FatherBrowsRaise = "jdh:john_father/brows_raise";
private FaceAnimationId FatherFullFaceHold = "jdh:john_father/full_face_hold";
```

И используются:

```csharp
Line("father", "Ты поздно вернулся.").SetAnimationFace(FatherAngryBrows);
Line("father", "Я ждал почти весь вечер.").StopFaceAnimation();
Line("john", "Я знаю.").SetAnimationFace(PlayerLookLeft);
Line("john", "Я не хотел будить дом.").StopFaceAnimation();
Line("father", "Скажи прямо, где ты был?").SetAnimationFace(FatherBrowsRaise);
Line("father", "Без красивых историй.").StopFaceAnimation();
Line("john", "Я могу объяснить.").SetAnimationFaceClear();
```

Практическое правило для сцен:

- эмоцию ставьте на той реплике, где она должна появиться;
- если эмоция должна держаться на следующей реплике того же speaker, не ставьте stop сразу;
- если эмоция должна красиво уйти, следующей репликой того же speaker поставьте `.StopFaceAnimation()`;
- если переходите к другому actor/visual и нужно очистить всё, поставьте `.SetAnimationFaceClear()`;
- не используйте `SetAnimationFaceClear()` вместо `StopFaceAnimation()`, если в animation есть хорошо написанный `Exit`.

## Телесная анимация из Dialogue

`NodeLineReader` также поддерживает:

```csharp
Line("father", "Сюда.").SetAnimation(FatherGesture);
```

Это компилируется в событие cue:

```text
bodyAnimation=<id>
```

Ожидаемый type field - `BodyAnimationId`.

На практике в текущем проекте body animation чаще запускается из Story через `echo_animation:play_animation`, потому что там есть await/stop/wait lifecycle. Dialogue body cue полезен для коротких жестов, привязанных к строке, если bridge/rendering сторона сцены это обрабатывает.

## Катсцена и анимация (Cutscene)

Вызовы cutscene читаются в `DialogueResource`, а не в `StoryResource` напрямую.

Поддерживаемые compact-вызовы:

```csharp
Cutscene.Start(BedtimeStory);
Cutscene.Start(BedtimeStory, holdLastFrame: true);
Cutscene.Start(BedtimeStory, hold: true);
Cutscene.Play("jdh:chapter_1/bedtime_story");
Cutscene.Wait(id: "jdh:chapter_1/bedtime_story");
Cutscene.WaitPrepared(BedtimeStory);
Cutscene.Stop(BedtimeStory);
```

Сопоставление:

```text
Cutscene.Start        -> echo_cutscene:start
Cutscene.Play         -> echo_cutscene:play
Cutscene.Wait         -> echo_cutscene:wait
Cutscene.WaitPrepared -> echo_cutscene:wait_prepared
Cutscene.Stop         -> echo_cutscene:stop
```

Почему это важно для авторинга анимаций: cutscene может держать камеру, пока Dialogue управляет facial-анимациями на портретах и/или 3D-subject. Если facial animation "не видна", проверьте, что камера/visual действительно показывает нужного subject.

Текущий content-pack ссылается на:

```text
jdh:story_telling_sister_005
```

При сканировании `mods` не найдено `.cutscene.es` или `.camera-track.es` для этого ID. Значит ресурс либо генерируется/лежит вне `mods`, либо ещё не добавлен.

## Полная цепочка NPC + тело + лицо + диалог

Минимальный комплект персонажа:

```text
NpcResource
NpcAnimationBindingResource
AnimationProfileResource
AnimationRigResource
FaceRigResource
PortraitOverlayConfig
FacialAnimationResource
DialogueResource alias
DialogueResource Line(...).SetAnimationFace(...)
```

Шаблон:

```csharp
// data/jdh/echo_npc/npcs/chapter_1/family/new_character.npc.es
namespace JDH.Npc.Chapter1.Family;

class NewCharacterNpc : NpcResource
{
    public override NpcDef Build()
    {
        return Npc(id: "jdh:new_character",
            display: "Новый персонаж",
            skin: Skin("jdh:new_character"));
    }
}
```

```csharp
// data/jdh/echo_npc_animation/bindings/chapter_1/family/new_character.npc-animation-binding.es
namespace JDH.NpcAnimation.Chapter1.Family;

class NewCharacterAnimationBinding : NpcAnimationBindingResource
{
    public override NpcAnimationBindingDef Build()
    {
        return NpcAnimationBinding("jdh:new_character_standard_humanoid",
            npc: "jdh:new_character",
            profile: "jdh:standard_humanoid");
    }
}
```

```csharp
// data/jdh/echo_animation/face_rigs/chapter_1/family/new_character.face-rig.es
namespace JDH.Animation.Chapter1.Family;

face NewCharacter("jdh:new_character")
{
    Eyes
    {
        Sclera Left  { Position(-0.125, 0, 0); }
        Sclera Right { Position( 0.125, 0, 0); }
        Pupil Left   { Position(-0.09, 0, 0.01); }
        Pupil Right  { Position( 0.09, 0, 0.01); }
        Eyelid Upper { Position(0, -0.035, 0.02); TextureArea(8, 13, 8, 1); }
        Eyelid Lower { Position(0, 0.09, 0.02); TextureArea(8, 12, 8, 1); }
    }

    Brows
    {
        Pivot(-62, 0);
        Left  { Position(-0.125, -0.032, 0.03); }
        Right { Position( 0.124, -0.032, 0.03); Pivot(62, 0); }
    }

    Blink "eye_closure"
    {
        Travel(35, -35);
        Gaze(1, 1);
        Idle(true);
        PeriodTicks(30);
        CloseTicks(2);
        HoldTicks(2);
        OpenTicks(2);
    }
}
```

```csharp
// story/configs/jdh/echo_animation/portrait_overlays/new_character.portrait-overlay.es
namespace JDH.Configs.EchoAnimation;

class NewCharacterPortraitOverlay : PortraitOverlayConfig
{
    public override PortraitOverlayDef Build()
    {
        return PortraitOverlay("jdh:new_character",
            centerX: "0.50",
            centerY: "0.49",
            offsetX: 0,
            offsetY: 0,
            facePixels: "4",
            layers: [
                Layer("sclera_left", placeX: "-0.062", placeY: "0.002", scaleX: "0.50", scaleY: "0.50"),
                Layer("sclera_right", placeX: "0.062", placeY: "0.002", scaleX: "0.50", scaleY: "0.50"),
                Layer("pupil_left", placeX: "-0.062", placeY: "0.002", scaleX: "0.50", scaleY: "0.50"),
                Layer("pupil_right", placeX: "0.062", placeY: "0.002", scaleX: "0.50", scaleY: "0.50"),
                Layer("upper_eyelid_left", placeX: "-0.062", placeY: "-0.034", scaleX: "0.50", scaleY: "0.50"),
                Layer("upper_eyelid_right", placeX: "0.062", placeY: "-0.034", scaleX: "0.50", scaleY: "0.50"),
                Layer("lower_eyelid_left", placeX: "-0.062", placeY: "0.034", scaleX: "0.50", scaleY: "0.50"),
                Layer("lower_eyelid_right", placeX: "0.062", placeY: "0.034", scaleX: "0.50", scaleY: "0.50"),
                Layer("brow_left", placeX: "-0.062", placeY: "-0.029", scaleX: "0.50", scaleY: "0.50"),
                Layer("brow_right", placeX: "0.062", placeY: "-0.029", scaleX: "0.50", scaleY: "0.50")
            ]
        );
    }
}
```

```csharp
// data/jdh/echo_animation/face_animations/chapter_1/family/new_character/brows_raise.face-animation.es
namespace JDH.Animation.Chapter1.Family;

class NewCharacterBrowsRaiseAnimation : FacialAnimationResource
{
    public override FacialAnimationDef Build()
    {
        return FacialAnimation("jdh:new_character/brows_raise",
            rig: "jdh:new_character",
            surfaces: [World, Portrait]);
    }
}

class World : FacialAnimationSurface3d
{
    private void Enter()
    {
        Phase(durationTicks: 5, end: "HOLD", tracks: [
            Track("brows_left", frames: [Frame(0).Position(0, 0, 0), Frame(5).Position(0, "-0.018", 0)]),
            Track("brows_right", frames: [Frame(0).Position(0, 0, 0), Frame(5).Position(0, "-0.018", 0)])
        ]);
    }

    private void Stay()
    {
        Phase(durationTicks: 12, end: "HOLD", tracks: [
            Track("brows_left", frames: [Frame(0).Position(0, "-0.018", 0), Frame(12).Position(0, "-0.018", 0)]),
            Track("brows_right", frames: [Frame(0).Position(0, "-0.018", 0), Frame(12).Position(0, "-0.018", 0)])
        ]);
    }

    private void Exit()
    {
        Phase(durationTicks: 7, end: "RETURN_TO_BASE", tracks: [
            Track("brows_left", frames: [Frame(0).Position(0, "-0.018", 0), Frame(7).Position(0, 0, 0)]),
            Track("brows_right", frames: [Frame(0).Position(0, "-0.018", 0), Frame(7).Position(0, 0, 0)])
        ]);
    }
}

class Portrait : FacialAnimationSurfacePortrait
{
    private void Enter()
    {
        Phase(durationTicks: 5, end: "HOLD", tracks: [
            Track("brows_left", frames: [Frame(0).Position(0, 0, 0), Frame(5).Position(0, "-0.010", 0)]),
            Track("brows_right", frames: [Frame(0).Position(0, 0, 0), Frame(5).Position(0, "-0.010", 0)])
        ]);
    }

    private void Stay()
    {
        Phase(durationTicks: 12, end: "HOLD", tracks: [
            Track("brows_left", frames: [Frame(0).Position(0, "-0.010", 0), Frame(12).Position(0, "-0.010", 0)]),
            Track("brows_right", frames: [Frame(0).Position(0, "-0.010", 0), Frame(12).Position(0, "-0.010", 0)])
        ]);
    }

    private void Exit()
    {
        Phase(durationTicks: 7, end: "RETURN_TO_BASE", tracks: [
            Track("brows_left", frames: [Frame(0).Position(0, "-0.010", 0), Frame(7).Position(0, 0, 0)]),
            Track("brows_right", frames: [Frame(0).Position(0, "-0.010", 0), Frame(7).Position(0, 0, 0)])
        ]);
    }
}
```

```csharp
// data/jdh/echo_dialogue/dialogues/chapter_1/home/example.dialogue.es
namespace JDH.Dialogue.Chapter1.Home;

class ExampleDialogue : DialogueResource
{
    private FaceAnimationId CharacterBrowsRaise = "jdh:new_character/brows_raise";

    public override DialogueDef Build()
    {
        return Dialogue("jdh:example_dialogue");
    }

    private void Aliases()
    {
        Alias("char", "jdh:new_character", display: "Новый персонаж", visual: Portrait("jdh:new_character"));
    }

    private void Nodes()
    {
        Start("start");
        Line("char", "Ты это видел?").SetAnimationFace(CharacterBrowsRaise);
        Line("char", "Ладно, потом объясню.").StopFaceAnimation();
    }
}
```

## Как написать новую ONCE body-анимацию

1. Создать enter clip, main clip, exit clip.
2. Убедиться, что все кости из main есть в enter и exit.
3. Убедиться, что конец enter равен старту main.
4. Убедиться, что старт exit равен концу main.
5. Убедиться, что конец exit возвращает все animated bones в base pose.
6. Создать `AnimationResource` с `mode: "ONCE"`.
7. В Story запускать с `await: "COMPLETED"`.

Шаблон:

```csharp
namespace JDH.Animation.Chapter1.Family;

class NodOnceAnimation : AnimationResource
{
    public override AnimationDef Build()
    {
        return Animation("jdh:nod_once",
            channels: [Channel("body")],
            sources: [Source("body/idle")],
            entries: [Entry("body/idle", clip: "jdh:nod_enter", durationTicks: 3)],
            main: Main(clip: "jdh:nod_main", mode: "ONCE", durationTicks: 18,
                exitPoints: [ExitPoint("complete", progress: "1.0")]),
            exits: [Exit("complete", clip: "jdh:nod_exit", durationTicks: 3)],
            target: "body/idle",
            recovery: Recovery("BOUNDED", checkpointIntervalTicks: 6),
            replay: "REJECT_IF_RUNNING");
    }
}
```

Использование в Story:

```csharp
Action(
    "play_nod",
    type: "echo_animation:play_animation",
    animation: "jdh:nod_once",
    target: "jdh:john_father",
    await: "COMPLETED",
    output: "nod_result",
    next: "after_nod");
```

## Как написать новую LOOP body-анимацию

1. Enter переводит из idle в начало loop.
2. Main loop должен быть бесшовным: поза в `0.0` и поза в `1.0` должны совпадать или визуально продолжаться.
3. Для `LOOP_UNTIL_STOP` нужен `ExitPoint`.
4. Exit возвращает к base pose.
5. Story запускает с `await: "STARTED"`.
6. Когда loop больше не нужен, Story вызывает `stop_animation`.
7. После stop можно вызвать `wait_animation`, если надо дождаться терминального состояния.

Шаблон:

```csharp
return Animation("jdh:hold_object",
    channels: [Channel("body")],
    sources: [Source("body/idle")],
    entries: [Entry("body/idle", clip: "jdh:hold_object_enter", durationTicks: 6)],
    main: Main(clip: "jdh:hold_object_loop", mode: "LOOP_UNTIL_STOP", durationTicks: 20,
        exitPoints: [ExitPoint("neutral", progress: "0.0")]),
    exits: [Exit("neutral", clip: "jdh:hold_object_exit", durationTicks: 6)],
    target: "body/idle",
    recovery: Recovery("BOUNDED", checkpointIntervalTicks: 8),
    replay: "REJECT_IF_RUNNING");
```

Использование в Story:

```csharp
Action(
    "start_hold",
    type: "echo_animation:play_animation",
    animation: "jdh:hold_object",
    target: "jdh:john_father",
    await: "STARTED",
    next: "story_continues");

Action(
    "stop_hold",
    type: "echo_animation:stop_animation",
    animation: "jdh:hold_object",
    target: "jdh:john_father",
    output: "hold_stop",
    next: "wait_hold");

Action(
    "wait_hold",
    type: "echo_animation:wait_animation",
    animation: "jdh:hold_object",
    target: "jdh:john_father",
    output: "hold_terminal",
    next: "finish");
```

## Инвентарь ассетов

Скин NPC:

```text
assets/jdh/textures/echo_npc/skins/john_father.png
```

Портреты Dialogue:

```text
assets/jdh/textures/echo_dialogue/portraits/john_father.png
```

Рабочий банк skin/source-ассетов:

```text
D:\JDHSeason\mods\echo-story-engine\skins
```

При сканировании найдено около 1984 PNG в `mods\echo-story-engine\skins`. Категории включают:

```text
body
brows
eyelids
glasses
portraits
pupil
sclera
```

Есть группы для `john/base/classic/combo/new`, `npc` и другие skin/source варианты. Это полезно как банк ассетов при сборке новых face rigs и portraits.

## Отладочные команды

Найденные команды/паттерны:

```text
/echo_animation face_rig jdh:john_father
/echo_animation acceptance face jdh:john_father neutral
/echo_animation acceptance face jdh:john_father look_left
/echo_animation acceptance face jdh:john_father brows_raise
/echo_animation acceptance face jdh:john_father angry_brows
/echo_animation acceptance face jdh:john_father full_face_hold
/echo_npc spawn jdh:john jdh:john_father ~ ~ ~
```

Команды искать в:

```text
mods/echo-animation/animation-forge-1.20.1/src/main/java/.../EchoAnimationCommands.java
mods/echo-animation/animation-forge-1.20.1/src/main/java/.../EchoAnimationAcceptanceCommands.java
mods/echo-animation/animation-forge-1.20.1/src/main/java/.../EchoAnimationFaceAcceptanceCommands.java
mods/echo-npc-animation-adapter/src/main/java/.../AdapterAnimationCommands.java
```

Практический порядок проверки:

1. Проверить, что rig загрузился:

```text
/echo_animation face_rig jdh:john_father
```

2. Проверить neutral pose:

```text
/echo_animation acceptance face jdh:john_father neutral
```

3. Проверить каждую facial animation:

```text
/echo_animation acceptance face jdh:john_father angry_brows
```

4. Spawn NPC:

```text
/echo_npc spawn jdh:john_father jdh:john_father ~ ~ ~
```

5. Запустить story/dialogue, где есть `SetAnimationFace(...)`.

## Самые полезные rg-поиски

Найти все facial-анимации:

```powershell
rg -n "FacialAnimation\(" D:\JDHSeason\mods\echo-story-content-pack\src\main\resources -g "*.es"
```

Найти все face rig-и:

```powershell
rg -n "^face " D:\JDHSeason\mods\echo-story-content-pack\src\main\resources -g "*.es"
```

Найти все использования face cues:

```powershell
rg -n "SetAnimationFace|StopFaceAnimation|SetAnimationFaceClear" D:\JDHSeason\mods\echo-story-content-pack\src\main\resources -g "*.es"
```

Найти body animation actions:

```powershell
rg -n "echo_animation:(play_animation|stop_animation|wait_animation)" D:\JDHSeason\mods\echo-story-content-pack\src\main\resources -g "*.es"
```

Найти все body animation resources:

```powershell
rg -n "return Animation\(" D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\data\jdh\echo_animation -g "*.es"
```

Найти tracks по конкретной кости:

```powershell
rg -n "Track\(\"brows_left\"|Track\(\"head\"|Track\(\"right_forearm\"" D:\JDHSeason\mods -g "*.es"
```

Найти портретные overlay-слои:

```powershell
rg -n "PortraitOverlay\(" D:\JDHSeason\mods\echo-story-content-pack\src\main\resources -g "*.es"
```

Найти ссылки на cutscene:

```powershell
rg -n "Cutscene\.|CutsceneId" D:\JDHSeason\mods -g "*.es" -g "*.java"
```

## Частые ошибки и быстрые исправления

Ошибка: facial animation не применяется к NPC.

Проверить:

```text
FacialAnimation rig == FaceRig ID
Alias visual Portrait ID == FaceRig ID
Track bones exist in FaceRig
surfaces contains World and/or Portrait
```

Ошибка:

```csharp
return FacialAnimation("jdh:new_character/brows_raise",
    rig: "jdh:wrong_character",
    surfaces: [World, Portrait]);
```

Исправление:

```csharp
return FacialAnimation("jdh:new_character/brows_raise",
    rig: "jdh:new_character",
    surfaces: [World, Portrait]);
```

Ошибка: в animation двигается `brow_left`, но ничего не происходит.

Причина: `brow_left` - ID слоя. Кость называется `brows_left`.

Неправильно:

```csharp
Track("brow_left", frames: [
    Frame(0).Position(0, 0, 0),
    Frame(5).Position(0, "-0.018", 0)
])
```

Правильно:

```csharp
Track("brows_left", frames: [
    Frame(0).Position(0, 0, 0),
    Frame(5).Position(0, "-0.018", 0)
])
```

Ошибка: portrait не показывает facial animation, а 3D показывает.

Проверить:

```text
FacialAnimation surfaces: [World, Portrait]
PortraitOverlay("same_id_as_visual")
Dialogue Alias visual: Portrait("same_id_as_face_rig")
Layer IDs in PortraitOverlay match generated Layer IDs
```

Ошибка: loop-анимация не заканчивается.

Проверить:

```text
Story вызывает stop_animation
После stop при необходимости вызывает wait_animation
main mode не ONCE
await на play был STARTED, не COMPLETED
```

Ошибка: валидатор ругается на coverage.

Проверить:

```text
Все кости из main clip есть в entry clip
Все кости из main clip есть в exit clip
Даже нулевой Track нужен, если bone есть в main
```

Ошибка: validator ругается на несовпадение поз фаз.

Проверить:

```text
end pose entry == start pose main
start pose exit == sampled main clip ? exitPoint
end pose exit == base pose
```

Ошибка: face rotation не компилируется.

Неправильно:

```csharp
Frame(6).Rotation("-4.0", 0, 0)
```

Правильно:

```csharp
Frame(6).Rotation(0, 0, "-4.0")
```

Ошибка: blink не работает.

Проверить:

```text
Есть Eyelid Upper
Есть Eyelid Lower
Blink idle true
Control id совпадает: eye_closure
Если активная facial animation suppressIdleBlink: true, idle blink будет подавлен
```

## Практические правила именования

Для NPC:

```text
jdh:<character>
jdh:<character>/<emotion>
jdh:<character>_standard_humanoid
```

Пример:

```text
jdh:john_father
jdh:john_father/angry_brows
jdh:john_father_standard_humanoid
```

Для player:

```text
echo_content_player:player/base       // face rig and portrait overlay
jdh:player/look_left                  // facial animation
jdh:player/brows_raise                // facial animation
```

Для body animations:

```text
jdh:wave
jdh:wave_enter
jdh:wave_loop
jdh:wave_exit
```

Для one-shot:

```text
jdh:look_around_once
jdh:look_around_enter
jdh:look_around_main
jdh:look_around_exit
```

## Чеклист перед добавлением эмоции в сюжет

1. Есть PNG слои лица в `assets/jdh/textures/echo_animation/faces/<character>`.
2. Есть portrait PNG в `assets/jdh/textures/echo_dialogue/portraits`.
3. Есть `face <Class>("jdh:<character>")`.
4. Face rig генерирует нужные кости: `brows_left`, `brows_right`, `left_pupil`, `right_pupil`, `upper_eyelids`, `lower_eyelids`.
5. Есть `PortraitOverlay("jdh:<character>")`.
6. В overlay ID слоев совпадают со сгенерированными LayerId.
7. Есть `FacialAnimation("jdh:<character>/<emotion>", rig: "jdh:<character>", surfaces: [World, Portrait])`.
8. В `Track(...)` используются BoneId, не LayerId.
9. В Dialogue alias стоит `visual: Portrait("jdh:<character>")`.
10. В Dialogue line стоит `.SetAnimationFace(CharacterEmotion)`.
11. Следующая line использует `.StopFaceAnimation()` или `.SetAnimationFaceClear()`, если эмоция должна уйти.

## Чеклист перед добавлением body-анимации

1. NPC имеет `NpcAnimationBinding`.
2. Binding указывает на profile.
3. Profile указывает на rig.
4. Rig содержит нужные body-кости.
5. Main clip содержит все tracks, которые реально двигаются.
6. Entry и exit clips покрывают все кости main clip.
7. Entry end pose совпадает с main start pose.
8. Exit start pose совпадает с main pose в exit point.
9. Exit end pose возвращается в base pose.
10. `Animation(...)` имеет корректный `MainMode`.
11. Story для `ONCE` использует `await: "COMPLETED"`.
12. Story для циклических/удерживаемых анимаций использует `await: "STARTED"`, затем `stop_animation`.

## Кодовые точки для разработчиков

Полезные Java files:

```text
mods/echo-animation/animation-contracts/src/main/java/io/github/kxyr0/echoanimation/api/rig/definition/SegmentedHumanoidBone.java
mods/echo-animation/animation-contracts/src/main/java/io/github/kxyr0/echoanimation/api/id/BoneId.java
mods/echo-animation/animation-echoscript-adapter/src/main/java/io/github/kxyr0/echoanimation/echoscript/face/FaceRigConventions.java
mods/echo-animation/animation-echoscript-adapter/src/main/java/io/github/kxyr0/echoanimation/echoscript/face/FaceRigLowerer.java
mods/echo-animation/animation-definition/src/main/java/io/github/kxyr0/echoanimation/definition/validation/FaceRigValidator.java
mods/echo-animation/animation-definition/src/main/java/io/github/kxyr0/echoanimation/definition/validation/FacialAnimationValidator.java
mods/echo-animation/animation-definition/src/main/java/io/github/kxyr0/echoanimation/definition/validation/AnimationDefinitionValidator.java
mods/echo-animation/animation-echoscript-adapter/src/main/java/io/github/kxyr0/echoanimation/echoscript/binding/ClipCallReader.java
mods/echo-animation/animation-echoscript-adapter/src/main/java/io/github/kxyr0/echoanimation/echoscript/frame/FrameTransformReader.java
mods/echo-animation/animation-echoscript-adapter/src/main/java/io/github/kxyr0/echoanimation/echoscript/lifecycle/AnimationDefinitionCallReader.java
mods/echo-animation/animation-forge-api-1.20.1/src/main/java/io/github/kxyr0/echoanimation/forge/api/AnimationService.java
mods/echo-animation/animation-forge-1.20.1/src/main/java/io/github/kxyr0/echoanimation/forge/client/DialogueVisualFaceLayerBridge.java
mods/echo-animation/animation-forge-1.20.1/src/main/java/io/github/kxyr0/echoanimation/forge/client/DialoguePortraitAnimationState.java
mods/echo-dialogue/dialogue-echoscript-adapter/src/main/java/io/github/kxyr0/echodialogue/echoscript/binding/NodeLineReader.java
mods/echo-story-animation-adapter/src/main/java/io/github/kxyr0/echostoryanimationadapter/bridge/AnimationBridgeIds.java
mods/echo-story-animation-adapter/src/main/java/io/github/kxyr0/echostoryanimationadapter/handler/AnimationActionHandler.java
```

`AnimationService` показывает полезные runtime-концепции. Реальные имена методов в текущем API:

```java
List<FaceRigId> faceRigs();
List<FacePoseId> facePoses();
List<FacialAnimationId> facialAnimations();
List<AnimationRigId> animationRigs();
List<AnimationClipId> animationClips();
List<AnimationId> animations();
Optional<MainMode> animationMainMode(AnimationId animationId);
List<AnimationProfileId> animationProfiles();

boolean ensureSubject(AnimationSubjectId subjectId);
AnimationSubjectProvisioningResult ensureSubject(AnimationSubjectProvisioningRequest request);
List<AnimationSubjectId> subjectsByOwner(AnimationSubjectLifecycleOwnerId ownerId);
boolean retireSubject(AnimationSubjectId subjectId, AnimationSubjectLifecycleOwnerId ownerId);

AnimationPlayResult playAnimation(AnimationId animationId, AnimationSubjectId subjectId, AnimationCommandId commandId);
AnimationPlayResult playAnimation(AnimationId animationId, AnimationSubjectId subjectId, AnimationCommandId commandId, double playbackSpeed);
AnimationStopResult stopAnimation(AnimationSubjectId subjectId, Optional<AnimationId> animationId, AnimationCommandId commandId);
boolean updateAnimationPlaybackSpeed(AnimationSubjectId subjectId, AnimationId animationId, AnimationCommandId commandId, double playbackSpeed);
AnimationPlaybackQueryResult queryAnimation(AnimationSubjectId subjectId, AnimationId animationId);
```

Смысл API: ID каталогов, подготовка subjects, play/stop/query для body animations, sync snapshots для клиентов и доступ к каталогу facial-анимаций.

## Самое важное для поиска

Ищите по этим строкам внутри `animation.md`:

```text
body-кости
face bones
generated face layers
brows_left
brow_left
upper_eyelids
upper_eyelid_left
left_pupil
SetAnimationFace
StopFaceAnimation
SetAnimationFaceClear
echo_animation:play_animation
echo_animation:stop_animation
echo_animation:wait_animation
PortraitOverlay
jdh:john_father
echo_content_player:player/base
jdh:wave
jdh:look_around_once
MISSING_ENTRY_BONE_COVERAGE
ANIMATION_PHASE_POSE_MISMATCH
EXIT_TARGET_POSE_MISMATCH
```

## Короткая ментальная модель

Body animation:

```text
NPC -> NpcAnimationBinding -> AnimationProfile -> AnimationRig -> AnimationClip tracks -> Animation lifecycle -> Story action
```

Face animation:

```text
Dialogue Alias visual -> FaceRig -> FaceLayer textures -> PortraitOverlay -> FacialAnimation surfaces -> Line cue
```

Самая частая причина поломки body animation - target не имеет profile или entry/main/exit clips не совпадают по костям/pose.

Самая частая причина поломки facial animation - перепутан `BoneId` и `LayerId`, либо не совпали `visual ID`, `FaceRig ID`, `PortraitOverlay ID`, `FacialAnimation rig`.
