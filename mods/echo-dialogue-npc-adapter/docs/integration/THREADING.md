# Threading

Echo Dialogue calls presentation providers from its authoritative server-side
runtime path. Echo NPC public service is documented as server-thread owned in
Forge.

The adapter:

- does synchronous lookup and query only;
- starts no thread;
- schedules no async work;
- caches no actor view;
- keeps only bounded warning keys.
