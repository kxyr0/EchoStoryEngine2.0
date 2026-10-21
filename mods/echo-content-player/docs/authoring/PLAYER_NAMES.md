# Имена игрока

Account name берётся из Minecraft `GameProfile` и не мутируется модом. Echo display name — отдельная presentation identity для Echo UI, диалогов, сюжета и будущего wardrobe/profile UI.

Режимы:

- `ACCOUNT`: resolved display name равен текущему account name. Смена имени аккаунта обновится при следующем login без создания custom mutation.
- `CUSTOM`: resolved display name равен сохранённому `PlayerDisplayName`. Смена имени аккаунта не меняет custom display name.

Правила `CUSTOM`:

- Unicode разрешён;
- пустое имя и имя из одних пробелов запрещены;
- control characters запрещены;
- максимум 48 Unicode code points;
- leading/trailing spaces удаляются;
- HTML, MiniMessage и arbitrary formatting не входят в P1.
