# Player Sleep Transitions

Сервер меняет status через `PlayerSleepSessionTransition`.

Клиент получает `EyeTransitionMessage` через `PlayerSleepNetwork`. `ClientSleepTransitionMirror` хранит только immutable transition параметры, а `PlayerSleepOverlayRenderer` рисует framebuffer-independent overlay.

Coverage не зависит от GUI Scale. Renderer получает actual overlay width/height и заполняет всё окно при полном закрытии.
