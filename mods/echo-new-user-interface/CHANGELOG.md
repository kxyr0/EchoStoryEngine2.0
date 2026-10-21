# Changelog

## 0.1.1

- Отключено сглаживание texture книги при preview.
- На левую страницу добавлены пункты `Начать игру`, `Настройки`, `Выход в главное меню`.

## 0.1.0

- Создан новый независимый Forge 1.20.1 мод `echo-new-user-interface`.
- Добавлен pure Java layout для центрированной книги в физических пикселях framebuffer.
- Добавлена клиентская команда `/echo_new_ui debug book`.
- Добавлен client-only overlay с full texture blit PNG `196 x 153`.
- Добавлено подавление остальных HUD overlays во время preview.
- Добавлены unit tests для resolution matrix и GUI Scale invariance.
