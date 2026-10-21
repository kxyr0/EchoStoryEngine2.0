# Echo Void Magic: Epic Fight Adapter

Отдельный Forge 1.20.1 адаптер для `jdh:void_sword`.

- при реальном уроне создаёт короткий пустотный разрез на цели;
- в обычном бою эффект попадания работает через Forge damage event;
- в режиме Epic Fight след клинка запускается по состоянию attack animation и её времени;
- попадание в режиме Epic Fight подключено к его серверному `DEAL_DAMAGE_EVENT_DAMAGE`, поэтому разрез появляется именно после подтверждённого урона;
- Forge- и Epic Fight-события одного удара объединяются, чтобы частицы не удваивались;
- сам адаптер не содержит копий классов Epic Fight, Echo Magic или Echo Void Magic.

Зависимости: Echo Magic 0.5.3+, Echo Void Magic 0.2.1+, Epic Fight 20.14.17.
