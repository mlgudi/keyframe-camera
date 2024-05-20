# Keyframe Camera
Create and play camera sequences in RuneLite.

https://github.com/mlgudi/rl-plugins/assets/46876568/e8cc28f7-90d9-4dab-ac48-3beace239c55

## Usage

Move your camera to the desired position and click the "Add Keyframe" button on the side panel. You can add as many keyframes as you want. The camera will interpolate between them when you press play.

You can select an easing type for each keyframe. The easing is applied between the position of the keyframe and the next.

You can also set the duration of the transition between keyframes in milliseconds.

You can duplicate, reorder, and delete keyframes. Sequences of keyframes can be saved to/loaded from a file.

If the rotation appears a bit jittery, this is likely a result of the camera pitch/yaw being limited to whole numbers. This is a limitation of the RuneLite API. Try to avoid very small rotations, long durations, or easing types which result in granular rotation of the camera.
