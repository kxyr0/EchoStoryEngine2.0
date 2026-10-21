# ES2 NPC и архив сюжетного пакета

- NPC: добавить class metadata Id/Display/Skin/NameColor/DefaultForm, inferred ID из resource path, сохранить старый Build и fingerprint.
- Проверить эквивалентность профилей и отказ для смешанных/невалидных metadata.
- Перевести NPC content-pack пофайлово с резервной копией live Prism.
- Story Forge: маркер .archived скрывает data/assets external pack и блокирует повторный seed.
- Content tools: Archive создаёт ZIP вне story и маркер; Restore снимает маркер без перезаписи рабочих файлов.
- Проверить Archive/Restore/Status на временном pack, выполнить Gradle check и собрать JAR.
