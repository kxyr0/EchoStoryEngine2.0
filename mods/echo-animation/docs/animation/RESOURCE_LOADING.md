# Resource loading

Forge discovery scans only:

- `echo_animation/face_rigs/**/*.face-rig.es`
- `echo_animation/face_poses/**/*.face-pose.es`
- `echo_animation/face_animations/**/*.face-animation.es`

It does not load `.json`, `.json5`, `.yaml`, `.toml`, `.properties` or Java classes as production animation definitions.

Invalid reload candidates do not replace the last known-good catalog.

