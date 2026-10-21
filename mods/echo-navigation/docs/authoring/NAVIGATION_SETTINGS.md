# Navigation Settings

Default runtime settings:

| Setting | Default |
| --- | ---: |
| backend | `echo_navigation:vanilla` |
| arrival tolerance | `0.6` |
| repath interval ticks | `20` |
| max replans | `3` |
| progress sample interval ticks | `10` |
| minimum progress | `0.1` |
| stuck ticks | `100` |
| max duration ticks | `12000` |

Runtime commands:

```text
/echo_navigation backend
/echo_navigation backend list
/echo_navigation backend set echo_navigation:vanilla
/echo_navigation backend set echo_navigation:astar
```

Rules:

- backend switch affects new movements only;
- active movements keep their pinned backend ID;
- selecting an unavailable backend is rejected;
- missing settings fall back to the documented defaults.

To test the custom route motor, set:

```toml
backend = "echo_navigation:astar"
```
