# Keyframe Camera
Create and play camera sequences in RuneLite.

## Usage

You must be logged in with free cam enabled to use the plugin. The side-panel will provide you with a button to enable it. You can press escape to disable free cam and return to normal gameplay.

Move your camera to the desired position and click the "Add Keyframe" button. You can add as many keyframes as you want. The camera will interpolate between them when you press play.

You can select an easing type for each keyframe. The easing is applied between the position of the keyframe and the next.

You can also set the duration of the transition between keyframes in milliseconds.

You can duplicate, reorder, and delete keyframes. You can also save/load sequences of keyframes.

If the rotation appears a bit jittery, this is likely a result of the camera pitch/yaw being limited to whole numbers. This is a limitation of the RuneLite API. Try to avoid very small rotations, long durations, or easing types which result in granular rotation of the camera.