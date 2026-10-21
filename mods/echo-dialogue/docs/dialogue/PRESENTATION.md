# Presentation D6

Целевой overlay:

- центрированный panel;
- line presentation в формате `[Name]: text`;
- choice presentation с numbered ordered rows под фактический текст вопроса;
- optional speaker visual слева от текста;
- readable background;
- no vanilla chat history;
- no input hints, NPC UI, voice или generic UI.

D6 вставляет server-side dynamic speaker presentation resolution перед network send. Клиент получает уже готовые поля: final display name, optional speaker name color и optional visual descriptor. Клиент не знает, пришли они из authored `Alias(...)` или от provider.

Panel layout, typewriter, fade/resize animation, input handling и visual rendering остаются client-only presentation state и не меняют server revision.

Реализовано:

- server read model `DialoguePresentation`;
- client-only centered overlay;
- animation presets from `io.github.kxyr0.echo:echo-ui-animations`;
- panel open fade/scale, panel close fade/scale, typewriter text reveal и smooth content resize;
- smooth content resize starts from the currently rendered panel size, so short text after long text shrinks without a one-frame jump;
- panel width follows measured text/choice content up to `MAX_TEXT_WIDTH`;
- panel height follows all wrapped text lines without a fixed line-count cap;
- optional visual column uses `VISUAL_SIZE` and `VISUAL_GAP`;
- line/prompt text is vertically centered inside the visual row when a portrait/head is present;
- if visual is missing, invalid or too narrow for current screen, layout falls back to text-only without empty slot;
- text fade is sampled separately from panel fade, so text has its own appear/disappear phase;
- Enter/right-click input bridge;
- CHOICE opens an invisible non-pausing screen that releases the mouse cursor and blocks player walking/camera input;
- mouse choice selection через ЛКМ или ПКМ по visible choice;
- keyboard choice selection через `1`, `2`, `3`, `4`;
- line wrapping по текущей ширине экрана;
- close/update handling;
- wait lock между input и новой server revision;
- stale same-session open/update ignored;
- stale close ignored;
- client state clear на disconnect/logout.

Настройки spacing и размеров находятся в `ClientDialogueOverlayStyle`:

- `CHOICE_SECTION_GAP` — расстояние между текстом вопроса и списком choices;
- `CHOICE_GAP` — расстояние между вариантами;
- `CHOICE_PADDING_X` и `CHOICE_PADDING_Y` — внутренний padding варианта;
- `PADDING_X` и `PADDING_Y` — внутренний padding основной panel.
- `MAX_TEXT_WIDTH` — максимальная ширина текста; короткие реплики дают более компактную panel;
- `VISUAL_SIZE` — квадратная область portrait/head;
- `VISUAL_GAP` — расстояние между visual и text/choices;
- `CROSSHAIR_PADDING_Y` — вертикальная protected zone вокруг прицела; panel сдвигается ниже неё;
- `TEXT_APPEAR_DELAY_MS` — задержка перед fade-in/typewriter текста после открытия panel;
- `TYPEWRITER_MS_PER_CHARACTER`, `TYPEWRITER_MIN_DURATION_MS`, `TYPEWRITER_MAX_DURATION_MS` — скорость печатания.

Пресеты анимаций находятся в отдельной библиотеке `echo-ui-animations`; Echo Dialogue только sample-ит готовые
`AnimationPresetCatalog` значения на client side.

Manual visual checklist:

1. Запустить client.
2. Открыть test world с commands enabled.
3. Выполнить `/reload`.
4. Выполнить `/echo_dialogue play sample:greeting`.
5. Проверить centered compact panel, строку `[Отец]: Где ты был?`, русский текст, читаемый contrast и отсутствие empty visual slot.
6. Проверить typewriter reveal: текст появляется постепенно, без добавления реплик в chat.
7. Проверить fade/scale in при открытии и fade/scale out при закрытии.
8. Проверить ПКМ и Enter: одно действие не перескакивает через несколько строк.
9. Удержать Enter: строка не должна uncontrollably skip.
10. Изменить размер окна: текст остаётся внутри panel.
11. Открыть pause menu и закрыть: текущая строка сохраняется.
12. Дойти до final line: overlay закрывается.
13. Вернуться в title/disconnect: stale overlay не остаётся.
14. Проверить chat history: dialogue line text не добавляется в vanilla chat.
15. Выполнить `/echo_dialogue play sample:manual_choice_1`.
16. Проверить, что вопрос `[Отец]: Выбери вариант проверки 1.` печатается сверху.
17. Проверить, что после вопроса panel плавно расширяется под `1. Текст выбора` ... `4. Текст выбора`.
18. Проверить, что `1`, `2`, `3`, `4` выбирают соответствующую ветку.
19. Проверить, что на CHOICE ходьба и поворот камеры заблокированы, а курсор мыши свободен.
20. Проверить, что ЛКМ и ПКМ по choice выбирают только эту ветку.
21. Выполнить `/echo_dialogue play sample:long_overflow`.
22. Проверить, что длинная реплика переносится внутри `MAX_TEXT_WIDTH`, показывается полностью по высоте и не перекрывает прицел ближе `CROSSHAIR_PADDING_Y`.
23. Выполнить `/echo_dialogue play sample:visual_showcase`.
24. Проверить, что `SkinHead("sample:father")` отображает лицо слева от `[Отец]`.
25. Нажать Enter и проверить, что `Portrait("sample:merchant")` отображается слева от `[Торговец]`.
26. Проверить, что текст `[Торговец]: ...` центрирован по Y относительно portrait/head row.
27. Дождаться choices и проверить, что portrait остаётся только у prompt speaker, а choice hitboxes начинаются справа от visual.
28. Проверить, что ЛКМ/ПКМ по portrait не выбирает choice, а ЛКМ/ПКМ по choice выбирает только соответствующий вариант.
29. На узком окне проверить deterministic text-only fallback без пустой visual области.

Если оператор не наблюдает пункты 5-29 напрямую, они должны быть отмечены как user-verified или pending, а не auto-passed.
