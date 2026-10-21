# Editor Preview Override

0.3.0 uses the `echo-animation-authoring` public preview hook. It stores the active draft snapshot as dev-only preview
state in authoring and clears it through `/echo_animator clear_preview` or Ctrl+X.

The editor viewport also applies the last preview delta to the local gizmo marker for immediate visual feedback.

This still does not mutate private Echo Animation runtime pose state. Runtime-side preview rendering should only be
added through a future Echo Animation public preview API.
