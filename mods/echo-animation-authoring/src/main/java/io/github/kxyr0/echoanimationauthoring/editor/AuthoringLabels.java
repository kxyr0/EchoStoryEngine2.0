package io.github.kxyr0.echoanimationauthoring.editor;

import java.util.List;

/**
 * Russian UI labels and tooltips used by the A0 editor surface.
 */
public final class AuthoringLabels {
    public static final String OPEN_EDITOR = "Открыть редактор Echo Animation";
    public static final String OPEN_WORKSPACE = "Открыть content-pack";
    public static final String HIERARCHY = "Иерархия костей";
    public static final String TRANSLATE = "Перемещение";
    public static final String PIVOT = "Пивот";
    public static final String RE_PARENT = "Сменить родителя";
    public static final String LAYERS = "Слои лица";
    public static final String HIDE_GLASSES = "Скрыть очки";
    public static final String COMPARE_DEFAULT = "Сравнить с исходной позой";
    public static final String TIMELINE = "Таймлайн анимации";
    public static final String EXPORT_NEUTRAL = "Экспорт neutral skin";
    public static final String WORKSPACE_NOT_CONFIGURED = "Workspace не настроен";
    public static final String WRITES_BLOCKED = "Запись заблокирована";
    public static final String WRITES_ENABLED = "Запись включена";
    public static final String PREVIEW = "Превью головы";
    public static final String FRAME = "Кадр";

    private AuthoringLabels() {
    }

    public static void requireRussianLabels() {
        List<String> labels = List.of(OPEN_EDITOR, OPEN_WORKSPACE, HIERARCHY, TRANSLATE, PIVOT,
                RE_PARENT, LAYERS, HIDE_GLASSES, COMPARE_DEFAULT, TIMELINE, EXPORT_NEUTRAL,
                WORKSPACE_NOT_CONFIGURED, WRITES_BLOCKED, WRITES_ENABLED, PREVIEW, FRAME);
        if (labels.stream().anyMatch(label -> label.chars().noneMatch(ch -> ch >= 0x0400 && ch <= 0x04FF))) {
            throw new IllegalStateException("Authoring label должен содержать русский текст.");
        }
    }
}
