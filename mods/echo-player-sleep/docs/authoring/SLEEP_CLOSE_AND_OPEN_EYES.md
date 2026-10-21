# CloseEyes и OpenEyes

`Sleep.CloseEyes` переводит session в `CLOSING_EYES` или `CLOSING_FOR_RETURN` и отправляет клиенту `EyeTransitionMessage`.

Клиентский `PlayerSleepOverlayRenderer` рисует две чёрные маски век: верхняя идёт вниз, нижняя вверх. При coverage `1.0` дополнительно заливается весь framebuffer.

`Sleep.OpenEyes` отправляет обратный transition. `sleepyBlink` сохраняется в сообщении и готов для расширенного профиля анимации.

Timeline считается от elapsed time, а не от количества frames, поэтому 60/144/240 FPS дают один и тот же результат на одинаковом времени.
