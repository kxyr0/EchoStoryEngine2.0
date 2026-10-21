# Architecture

Echo Animation is one general animation owner. Facial animation is A0's first vertical slice; future body animation must extend this owner instead of creating a competing face/body runtime.

Dependency direction:

`contracts -> core -> definition -> echoscript-adapter -> forge`

Client branch:

`contracts -> core -> client-rendering -> forge`

Forge API branch:

`contracts -> forge-api -> forge`

A0 does not implement persistence, per-Actor state machines, NPC adapters, Story adapters, Dialogue animation, locomotion or navigation.

