# A0 authoring execution plan

1. Create independent dev-only repository.
2. Compile against public Echo Animation artifacts from `../echo-animation/build/integration-maven`.
3. Implement client-only entrypoint and Russian labels.
4. Implement workspace safety, migration and neutral export tools.
5. Verify tests, source shape, whitespace, Checkstyle, PMD and JAR contents.

Executed:

```powershell
.\gradlew.bat compileJava --console=plain
.\gradlew.bat test --console=plain
.\gradlew.bat check --console=plain
```

Pending manual:

- open dev client;
- open editor screen;
- inspect real 3D preview;
- test gizmos in-game.

