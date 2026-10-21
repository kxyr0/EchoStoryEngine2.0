# Isolated Testing D6

D6 staged isolated mods:

```powershell
.\gradlew.bat verifyPresentationProviderIntegrationArtifacts --console=plain
```

Output:

`D:\JDHSeason\mods\echo-dialogue\build\presentation-provider-smoke\mods`

Expected JARs:

- `echo-dialogue-0.1.0-SNAPSHOT-all.jar`;
- `echo-dialogue-presentation-test-0.1.0-SNAPSHOT.jar`.

Echo Dialogue-only smoke remains valid without the provider JAR. With the provider JAR present, the server should show:

- `EDLG-PRESENT-001`;
- `EDLG-PRESENT-003` for `echo_dialogue_test:main_provider`;
- `EDLG-PRESENT-003` for `echo_dialogue_test:ambiguity_provider`;
- `EDLG-PRESENT-002`.

Manual client acceptance is still visual and must be user-confirmed. the operator must not claim visual rendering, restart visual state or world-switch acceptance unless it was actually observed.
