# Effect Client Sync

Forge host выделяет client visual state и overlay/camera hooks отдельно от core.

Требование EF1: не отправлять visual packets каждый tick. Клиент должен получать definition/snapshot события и sample-ить локально. Эта часть отделена от server movement contribution.
