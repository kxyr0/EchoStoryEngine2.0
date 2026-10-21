# Event Capture

Forge event thread captures vanilla events into immutable provider inputs. Server thread owns snapshot mutation, trigger
instance mutation and outcome commit.

Event capture rules:

- no synthetic events on restart or recovery
- coalesce duplicate block interaction callbacks
- do not store mutable `Entity`, `ItemStack` or `Level` objects in result payload
- store IDs, primitive values, positions, dimensions and server tick
