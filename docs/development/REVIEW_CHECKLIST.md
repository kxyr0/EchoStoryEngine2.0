# Review Checklist

Answer these before final reporting:

1. Did a Minecraft or Forge dependency enter contracts/core?
2. Did concrete game-domain logic appear?
3. Was an abstraction added without a current use case?
4. Did composition become a business service?
5. Is any exception logged more than once?
6. Are any application-owned subsystem log files created?
7. Do docs match the code?
8. Do quality gates catch boundary violations?
9. Were checks weakened for a green build?
10. Are class, method, and package limits respected?
11. Can the Forge adapter be replaced without rewriting core?
12. Can runtime be unit-tested without Minecraft?
13. Were unrelated files changed?
14. Is there hidden mutable global state?
15. Are there temporary decisions without recorded debt?
