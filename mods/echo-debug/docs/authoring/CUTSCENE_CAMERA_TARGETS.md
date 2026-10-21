# Cutscene Camera Targets

Echo Debug 0.3.0 adds server commands for Echo Cutscene C2 authoring:

```text
/echo_debug cutscene mode FREE
/echo_debug cutscene mode LOOK_AT
/echo_debug cutscene mode LOOK_AT_WITH_OFFSET
/echo_debug cutscene target player EYES
/echo_debug cutscene target fixed 120.5 66 194.5
/echo_debug cutscene target clear
```

The command parser keeps `/echo_debug cutscene enter <id> [speed]` unchanged, so
`/echo_debug cutscene enter jdh:story_telling_sister_1 0.45` remains valid.

Echo Cutscene also accepts short authoring aliases:

```text
/echo_cutscene mode LOOK_AT
/echo_cutscene target fixed 120.5 66 194.5
/echo_cutscene target clear
```
