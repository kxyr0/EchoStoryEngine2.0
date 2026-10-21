# Карта проекта

`magic-contracts` не импортирует Minecraft/Forge. `magic-core` содержит plain Java state и math. `magic-forge-api-1.20.1` — публичная граница. `magic-script-adapter-1.20.1` использует официальный parser. `magic-forge-1.20.1` владеет миром, сетью, persistence, collision и rendering. Testmod не входит в production JAR.

Поток: key/EchoScript → `SpellCastService` → validation → `SpellCastInstance` → burst → `MagicProjectileEntity` → swept collision → `DamageSource` → receipt.

