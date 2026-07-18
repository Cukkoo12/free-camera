# Free Camera 3.0

Free Camera is a client-side Fabric mod for Minecraft 26.1.2. It provides detached camera modes, cinematic controls, recording, and simple camera paths without moving the real player or manipulating movement packets.

## Features

- Free Camera with accelerated six-axis movement and speed presets
- Orbit, Tripod, and Follow camera modes
- Responsive and Cinematic motion profiles
- Continuous camera roll and smooth hold-to-zoom
- Optional OFF, SOFT, and VANILLA-style camera collision
- Fixed-rate camera recording and playback
- Simple keyframe paths with easing, holds, looping, reverse, and ping-pong playback
- Built-in six-tab settings and Camera Studio screen
- Optional Mod Menu integration
- Configurable status HUD, hand, crosshair, HUD, view-bobbing, hurt-shake, and letterbox options

## Media

Screenshots and promotional media can be added here later without changing the rest of this page.

## Camera controls

- **F6 short press:** activate the preferred or last-used camera mode; press again to deactivate it.
- **Hold F6 for 0.30 seconds:** open the four-section Free / Orbit / Tripod / Follow radial menu.
- **F9:** toggle Cinematic motion.
- **F10:** open the built-in Free Camera Settings screen.
- **G:** select a loaded entity near the center of the view while Follow is active.
- **Z / C:** roll continuously. The angle remains when the key is released.
- **V:** hold smooth zoom. The default multiplier is 3×.
- **R:** if roll is present, reset only roll; press again while upright to reset the current camera mode.
- **Mouse wheel:** change Free Camera speed, Orbit radius, or Follow distance according to the active mode.

Free, Orbit, and Tripod direct shortcuts are available in Minecraft Controls and are unbound by default. Recording and last-recording playback shortcuts are also available but unbound by default. Every camera key can be rebound through Minecraft Controls.

## Camera modes

### Free

Move independently with camera-relative WASD, Space, and Shift controls. Movement is time-based, accelerates smoothly, normalizes diagonal input, and never moves the real player. Collision defaults to OFF.

### Orbit

Orbit around the local player with an independently requested radius. Collision can retract the rendered camera around obstructions and smoothly restore the requested radius when clear.

### Tripod

Capture and hold an exact visible camera pose. Tripod remains fixed until reset, switched, or deactivated and intentionally does not use collision adjustment.

### Follow

Follow defaults to the local player and supports Chase, Side, Fixed Angle, and Look At profiles. Use G to select an eligible loaded player, mob, armor stand, boat, minecart, or vehicle. If the target becomes unavailable, the current visible pose is preserved and the session safely switches to Free Camera.

## Cinematic motion, roll, zoom, and collision

Cinematic motion applies smoother acceleration, deceleration, rotation, roll, and zoom response without changing the real player. Roll rotates the complete world view while keeping the GUI upright and supports continuous full rotations. Zoom changes only the detached world projection and does not alter Minecraft's global FOV option.

Collision modes are configurable separately for Free, Orbit, Follow, and playback. Collision uses already-loaded client block collision shapes; it does not load chunks. Liquids and non-collidable plants are ignored. At unloaded chunk boundaries, enabled collision preserves the last safe visible position until collision data is available.

## Recording and playback

Camera recording stores the final visible detached-camera pose at a fixed time-based sample rate. The default is 30 samples per second with a ten-minute limit; settings allow 10–120 samples per second and bounded storage limits.

The Studio tab supports recording, stopping, saving, renaming, duplicating, deleting, looping, reverse playback, playback speed, and optional playback collision. Saved tracks preserve position, yaw, pitch, continuous roll, and zoom. Screens pause playback without advancing its timeline.

## Paths and keyframes

The path editor provides a simple move-and-add-keyframe workflow rather than a full video timeline. Keyframes store position, rotation, roll, zoom, duration, hold duration, and easing. Paths support linear or smooth position interpolation, Linear/Smooth/Cinematic easing, loop, reverse, ping-pong, and adjustable playback speed.

## Settings and Mod Menu

Press F10 to open the built-in General, Camera, Follow, Visuals, Studio, and Advanced tabs. The screen supports Apply, Cancel, Done, tab reset, full reset, preset previews, scrolling, keyboard focus, validation, and unsaved-change tracking.

Mod Menu support is optional. When Mod Menu is installed, its Configure button opens the same built-in screen. Free Camera does not require Mod Menu.

## Configuration and storage

- Main configuration: `config/free-camera.json`
- Saved recordings: `config/free-camera/recordings/`
- Saved paths: `config/free-camera/paths/`

Configuration and track files use bounded, versioned JSON. Saves use temporary files and atomic replacement where supported. Corrupt files are reported and preserved or backed up rather than silently deleted.

## Installation

1. Install Minecraft 26.1.2.
2. Install Fabric Loader 0.18.5 or newer for Minecraft 26.1.2.
3. Install Fabric API 0.145.4+26.1.2 or newer compatible with Minecraft 26.1.2.
4. Place `free-camera-3.0.0.jar` in the Minecraft `mods` folder.
5. Launch the Fabric profile. Use F6 in a world to start Free Camera.

The mod is client-side only and normally does not need to be installed on the server.

## Compatibility and safety

- Minecraft: exactly 26.1.2
- Fabric Loader: 0.18.5+
- Fabric API: 0.145.4+26.1.2+
- Java: 25+
- Environment: client-side

Free Camera does not write player position, velocity, rotation, bounding box, or `noPhysics`; modify entities; change key states; send or cancel packets; or force chunk loading. Gravity, vehicles, water, knockback, damage, and server movement continue to affect the real player normally.

## Troubleshooting

- If older options retain conflicting Z/C bindings, rebind or reset those controls in Minecraft Controls.
- If `free-camera.json` is corrupt, it is renamed with a `.corrupt` suffix and safe defaults are restored.
- If a configured key conflicts with another mod, change it through Minecraft Controls.
- At unloaded chunk boundaries, enabled collision waits for already-available client data instead of requesting chunks.

## Privacy

Free Camera does not transmit camera paths, recordings, configuration, or detached-camera movement. Recording and path files remain in the local Minecraft configuration directory unless the user copies them elsewhere.

## License

Free Camera is available under the MIT License.
