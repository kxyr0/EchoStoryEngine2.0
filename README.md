<div align="center">

# ECHO ENGINE

**Истории, персонажи и игровые системы для Minecraft.**

Модульный движок проекта **NO ECHO** для создания сюжетных приключений на Minecraft Forge.

![Minecraft 1.20.1](https://img.shields.io/badge/Minecraft-1.20.1-62B47A?style=flat-square)
![Forge](https://img.shields.io/badge/Loader-Forge-D9A066?style=flat-square)
![Java 17](https://img.shields.io/badge/Java-17-7D9DDF?style=flat-square)
![В разработке](https://img.shields.io/badge/Status-In_development-C4A7E7?style=flat-square)
[![Лицензия MIT](https://img.shields.io/badge/License-MIT-62B47A?style=flat-square)](LICENSE)

[Возможности](#возможности) · [Начало работы](#начало-работы) · [EchoScript](#echoscript) · [Модули](#модули) · [Документация](#документация)

</div>

---

Echo Engine объединяет сюжетные цепочки, диалоги, NPC, анимации, квесты и игровые механики в одной системе авторинга. Сценарии и описания контента хранятся в файлах `.es`, а отдельные модули отвечают за их выполнение в игре.

На движке можно собрать сцену встречи с персонажем, запустить разговор с выбором ответа, изменить состояние квеста, проиграть анимацию и связать происходящее с игровыми событиями. Для подготовки персонажей и анимаций в репозитории есть собственный редактор NPC, дополнение Blender и расширение VS Code.

> [!NOTE]
> Движок активно разрабатывается. Модули имеют собственные версии; API, форматы ресурсов и способы сборки могут меняться. Этот репозиторий содержит исходники и инструменты, а готовую конфигурацию игры нужно собирать с учётом зависимостей выбранных модулей.

## Возможности

| Область | Что входит |
| --- | --- |
| **Сюжет и квесты** | Последовательности действий, переменные, сохранение прогресса, отдельные состояния и цели квестов. |
| **Диалоги** | Реплики, варианты ответа, переходы, результаты разговора, портреты и представление текущего персонажа. |
| **Персонажи** | Описание NPC, формы и внешний вид, взаимодействие, перемещение и фоновые персонажи. |
| **Анимации и сцены** | Анимации тела и лица, профили и риги, постановка сцен, катсцены и управление актёрами. |
| **Игровые системы** | Магия и её школы, предметы, одежда, показатели игрока, HUD, время и сон. |
| **События и эффекты** | Доменные события, триггеры, реакции, визуальные эффекты, звуки и голосовое сопровождение. |
| **Инструменты** | Авторинг EchoScript в VS Code, редактирование NPC, импорт и экспорт в Blender, проверка модов. |

Связи между системами вынесены в отдельные адаптеры. Например, Story запускает диалог через Story–Dialogue, а отображение NPC в разговоре подключается через Dialogue–NPC. Состав установки определяется выбранными возможностями и их зависимостями.

## Начало работы

### Для автора контента

1. Откройте [руководство по контенту](mods/echo-story-content-pack/docs/authoring/START_HERE.md): оно объясняет, где описывать сюжет, диалоги, NPC и другие ресурсы.
2. Выберите нужный [шаблон](mods/echo-story-content-pack/authoring/templates) и создайте ресурс в соответствующей папке content pack.
3. Используйте [расширение VS Code](vscode/EchoTool/README.md) для подсветки, подсказок, переходов и проверки ссылок.
4. Подготовьте персонажей и анимации через [EchoNpcStudio](mods/EchoNpcStudio/README.md) и [Blender-дополнение](blender-addon/README.txt).
5. Проверьте контент средствами соответствующего модуля и в игре. Порядок работы описан в [руководстве автора](mods/echo-story-content-pack/docs/authoring/WORKFLOW.md).

### Для разработчика движка

Сохраните структуру репозитория: некоторые Gradle-проекты подключают соседние модули через `includeBuild`.

Для Java-модулей нужны **JDK 17**, доступ к репозиториям зависимостей и Gradle Wrapper из выбранного проекта. Отдельно устанавливать Gradle не требуется.

Пример проверки и сборки **EchoScript** из корня репозитория:

```powershell
cd mods/echo-script
.\gradlew.bat check build --console=plain
```

В Linux и macOS используется `./gradlew` вместо `.\gradlew.bat`; совместимость отдельных инструментов и сценариев запуска уточняйте в их документации.

Для Story Engine команды запускаются из его каталога:

```powershell
cd mods/echo-story-engine
.\gradlew.bat check build --console=plain
```

Каждый пример выше предполагает запуск из корня репозитория. Единой команды сборки всей платформы пока нет. Перед сборкой конкретного модуля проверьте его `README.md`, `settings.gradle`, `build.gradle` и `gradle.properties`: часть проектов использует Maven Local или JAR из локальной установки Minecraft.

## EchoScript

EchoScript — язык описания контента с C#-подобным синтаксисом. Его Java-библиотека разбирает `.es` в синтаксическое дерево; смысл конструкций и выполнение определяет соответствующая система движка.

Например, сюжет может запустить разговор и отметить его завершение:

```csharp
class ChapterOpeningStory : StoryResource
{
    OnStart()
    {
        Dialogue.Load("jdh:chapter_opening");
        Story.Set("chapter_1.opening_complete", true);
    }
}
```

Реплики описываются отдельным ресурсом:

```csharp
class ChapterOpeningDialogue : DialogueResource
{
    Node FirstMeeting()
    {
        Line("john", "Кажется, здесь начинается моя история.");
    }
}
```

В content pack эти примеры размещаются в разных папках:

```text
src/main/resources/data/jdh/
├── echo_story/stories/chapter_opening.es
└── echo_dialogue/dialogues/chapter_opening.dialogue.es
```

В этом примере ID сюжета и диалога — `jdh:chapter_opening`, а `john` обозначает говорящего `jdh:john`. Для показа разговора нужны соответствующие runtime-модули, Story–Dialogue Adapter и настройка говорящего.

Подробнее: [сюжеты](mods/echo-story-content-pack/docs/authoring/WRITING_STORIES.md) · [диалоги](mods/echo-story-content-pack/docs/authoring/WRITING_DIALOGUES.md) · [NPC](mods/echo-story-content-pack/docs/authoring/WRITING_NPCS.md) · [квесты](mods/echo-quests/docs/authoring/WRITING_QUESTS.md).

## Модули

Системы разделены по назначению. Внутри крупных модулей отдельно выделены контракты, ядро и интеграция с Forge; соединение с другими системами выполняют адаптеры.

| Группа | Основные проекты |
| --- | --- |
| **Язык и сюжет** | [EchoScript](mods/echo-script), [Story Engine](mods/echo-story-engine), [Dialogue](mods/echo-dialogue), [Quests](mods/echo-quests), [Content Pack](mods/echo-story-content-pack). |
| **Персонажи и движение** | [NPC](mods/echo-npc), [Background NPC](mods/echo-background-npc), [Navigation](mods/echo-navigation), [Player Actor](mods/echo-player-actor). |
| **Анимации и постановка** | [Animation](mods/echo-animation), [Cutscene](mods/echo-cutscene), [Scene](mods/echo-scene), [IK](mods/echo-ik). |
| **Механики и внешний вид** | [Magic](mods/echo-magic), [Item Pack](mods/echo-item-pack), [Wardrobe](mods/echo-wardrobe), [Player Vitals](mods/echo-player-vitals), [Player HUD](mods/echo-player-hud). |
| **Реакции и окружение** | [Events](mods/echo-events), [Trigger](mods/echo-trigger), [Time](mods/echo-time), [Effects](mods/echo-effects), [Audio](mods/echo-audio), [Voice](mods/echo-voice). |
| **Интеграции** | [Story–Dialogue](mods/echo-story-dialogue-adapter), [Story–NPC](mods/echo-story-npc-adapter), [Dialogue–NPC](mods/echo-dialogue-npc-adapter), [NPC–Animation](mods/echo-npc-animation-adapter), [NPC–Navigation](mods/echo-npc-navigation-adapter). |

Полный состав находится в [`mods/`](mods). Точные версии и зависимости задаются в файлах соответствующего проекта.

## Инструменты

| Инструмент | Назначение | Основа |
| --- | --- | --- |
| [EchoNpcStudio](mods/EchoNpcStudio/README.md) | Редактор NPC: скины, слои лица, портреты и сохранение в EchoScript. | Windows, C++20, MSVC, Windows SDK, CMake 3.24+. |
| [Echo Authoring Tools](vscode/EchoTool/README.md) | Расширение VS Code: подсветка, подсказки, навигация, форматирование и индекс ресурсов. | VS Code 1.85+, Node.js и npm для сборки. |
| [Echo Animation Exporter](blender-addon/README.txt) | Подготовка, импорт и экспорт анимаций, ригов и скинов. | Дополнение Blender; в метаданных указан минимум 4.3. |
| [Echo Mod Checker](echo_mod_checker/pyproject.toml) | Приложение проверки модов. | Python 3.11+, PySide6. |
| [Echo Platform Docs](docs/echo-platform-docs/README.md) | Портал документации с описанием систем и контрактов. | Astro, MDX и pnpm. |

## Структура репозитория

```text
.
├── mods/                  # Движок, игровые модули, адаптеры и EchoNpcStudio
├── vscode/EchoTool/        # Расширение VS Code
├── blender-addon/          # Дополнение Blender
├── echo_mod_checker/       # Приложение проверки модов
├── assets/                 # Исходные ресурсы
└── docs/                   # Документация и история изменений
```

## Документация

| С чего начать | Где читать |
| --- | --- |
| Создать контент | [Руководство автора](mods/echo-story-content-pack/docs/authoring/START_HERE.md). |
| Взять готовую форму ресурса | [Шаблоны content pack](mods/echo-story-content-pack/authoring/templates). |
| Разобраться в Story Engine | [Описание и архитектура](mods/echo-story-engine/README.md). |
| Разобраться в диалогах | [Описание Dialogue](mods/echo-dialogue/README.md), [текущий синтаксис](mods/echo-dialogue/docs/architecture/ECHOSCRIPT_HIGH_LEVEL_DIALOGUE.md). |
| Изучить остальные системы | [Исходники портала документации](docs/echo-platform-docs/src/content/docs). |
| Посмотреть изменения | [Changelog NO ECHO](docs/changelogs/README.md). |

Некоторые страницы портала описывают более ранние формы синтаксиса. Для новых сюжетов и диалогов используйте руководства content pack и актуальные шаблоны, указанные выше.

## Обратная связь и участие

При сообщении об ошибке укажите модуль и его версию, версию Forge, шаги воспроизведения, ожидаемый результат и фактическое поведение. Для ошибок контента приложите небольшой пример `.es` и относящиеся к нему сообщения диагностики.

При изменении движка сохраняйте границы систем и проверяйте затронутый Gradle-проект. Изменения формата ресурсов сопровождайте обновлением документации и примером для автора контента.

## Авторство и лицензия

Автор и основной разработчик — **kxyr0**. При разработке изредка помогал AI-ассистент ChatGPT (Codex).

Движок распространяется по [лицензии MIT](LICENSE). Copyright © 2026 kxyr0.

Существующие лицензии сторонних компонентов и материалов сохраняются и относятся к соответствующим файлам.
