# Animator Editor Session

`AnimatorForgeSession` владеет editor state, draft state, detached camera input and public API status snapshot.
Сессия создается только на клиенте командой `/echo_animator open` и освобождается через `close`, Esc or client logout.
