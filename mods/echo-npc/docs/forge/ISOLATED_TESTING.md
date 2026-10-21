# Isolated Final-JAR Testing

Isolated acceptance проверяет, что production JAR самодостаточен для N1 runtime.

## Запреты

В isolated instance нельзя использовать:

- sibling source sets;
- Maven Local artifacts;
- IDE run outputs;
- test classes;
- unpacked module classes.

## Server

Запустить Forge server с одним JAR:

```text
mods/echo-npc-0.1.0-SNAPSHOT-all.jar
```

Проверить `/echo_npc health`, `/echo_npc definitions`, spawn, teleport и despawn.

## Client

Запустить Forge client с тем же JAR. Проверить, что `sample:father` отображается
с authored skin, а definition без skin использует fallback texture.
