# Subject And Clip Selection

0.3.0 accepts subject and clip ids from the command line and asks authoring for live discovery. The discovery snapshot
contains public clip/rig ids when Echo Animation service is available and falls back to the authoring humanoid bone list
when it is not.

Use `/echo_animator select bone <bone_id>` or `/echo_animator select face <face_element_id>` to change the active target.
