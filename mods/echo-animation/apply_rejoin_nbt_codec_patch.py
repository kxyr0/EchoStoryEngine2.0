from pathlib import Path

path = Path("animation-forge-1.20.1/src/main/java/io/github/kxyr0/echoanimation/forge/playback/AnimationRuntimeNbtCodec.java")
if not path.exists():
    raise SystemExit(f"Не найден {path}. Запускай из корня echo-animation.")

text = path.read_text(encoding="utf-8")
call = "AnimationRuntimeTransientSanitizer.snapshotForPersistence(data)"
if call in text:
    print("OK: AnimationRuntimeNbtCodec уже содержит rejoin persistence sanitation.")
    raise SystemExit(0)

needle = "CompoundTag write(AnimationRuntimeSavedData data, CompoundTag root) {"
count = text.count(needle)
if count != 1:
    raise SystemExit(
        f"ОСТАНОВКА: ожидался ровно один метод write(...), найдено {count}. "
        "Файл не изменён — нужен ручной merge, чтобы не затереть новые правки."
    )

replacement = needle + "\n        data = AnimationRuntimeTransientSanitizer.snapshotForPersistence(data);"
patched = text.replace(needle, replacement, 1)
path.write_text(patched, encoding="utf-8")
print("OK: добавлена persistence sanitation строка. Остальной NBT codec не переписывался.")
