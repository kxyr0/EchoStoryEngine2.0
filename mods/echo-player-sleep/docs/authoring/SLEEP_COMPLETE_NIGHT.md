# CompleteNight

`Sleep.CompleteNight` вызывается явно автором сценария.

Мод подавляет vanilla night skip во время active sleep session через `SleepFinishedTimeEvent`, но не делает утро автоматически. Это защищает сценарии, где сон является воспоминанием, кошмаром или dream room без перехода к утру.

Текущая команда completion двигает day time исходного измерения к следующему дневному циклу.
