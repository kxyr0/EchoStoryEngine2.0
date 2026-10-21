# Wardrobe hot reload

Hot reload tracks profile, wearable, outfit, PNG, and model fingerprints. Invalid candidates do not replace the previous accepted catalog.

Publishing an accepted catalog immediately recomputes known player appearances from their current observed equipment. A `/reload` therefore keeps equipped clothing visible without requiring an item to be removed and equipped again.

The exact resolver never fills gaps by guessing. After reload, missing combinations are visible through:

- `/echo_wardrobe coverage`
- `/echo_wardrobe missing`
- `/echo_wardrobe diagnostics`

Empty production catalogs are accepted. Missing item registrations are warnings until restart-time registry discovery.
