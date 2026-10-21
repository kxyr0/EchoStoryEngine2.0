# Inline event subscriptions

Story frontend признаёт class-level When(predicate, id: "stable_name") { actions; }
как внешнюю декларацию. OnStart и граф Story не исполняют тело When. Типизированный
binder, индекс, архив pending revision и вызовы сервисов остаются в Echo Trigger
и Echo Events Integration; gameplay-зависимости в story-core не добавляются.

План файлов: HighLevelStoryResourceBinder, compiler tests, project map/progress;
Trigger inline binder, source collection и reload; Content Doctor; EchoTool;
авторское руководство и changelog. Проверить реальные parser/native compilers,
несколько обработчиков, стабильные IDs при reorder, invalid/reload LKG, сборку
и установленные JAR. Не считать наличие блока live-проверкой.
