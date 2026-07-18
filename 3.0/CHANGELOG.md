# Changelog

## 3.0.0 — 2026-07-19

### Camera modes

- Added safe Free, Orbit, Tripod, and Follow camera modes.
- Added Chase, Side, Fixed Angle, and Look At Follow profiles with loaded-entity target selection and pose-preserving target-loss fallback.
- Added pose-preserving direct mode switching and a four-section F6 radial menu.

### Camera movement and visuals

- Added frame-rate-independent movement with acceleration, deceleration, diagonal normalization, and five speed presets.
- Added Responsive and Cinematic motion profiles.
- Added continuous full-rotation camera roll, smooth 3× hold zoom, and detached-camera base-FOV isolation from player sprint and item-use effects.
- Added world-only roll and zoom composition with matching culling while keeping HUD and menus upright.
- Added configurable hand, crosshair, HUD, view-bobbing, hurt-camera, status overlay, control hint, and letterbox behavior.

### Collision

- Added OFF, SOFT, and VANILLA-style collision modes for Free, Orbit, Follow, and optional playback collision.
- Added immediate obstruction retraction, smooth safe recovery, unloaded-chunk safeguards, and already-loaded client collision queries without forced chunk loading.

### Settings and studio

- Added a built-in F10 settings screen with General, Camera, Follow, Visuals, Studio, and Advanced tabs.
- Added validation, Apply/Cancel/Done behavior, per-tab and full resets, preset previews, scrolling, keyboard focus, and optional Mod Menu integration.
- Added fixed-rate camera recording and time-based playback with pause, loop, reverse, speed, roll, zoom, and bounded duration/sample limits.
- Added a simple keyframe path editor with linear and smooth interpolation, easing, holds, reorder and edit operations, loop, reverse, ping-pong, and adjustable playback speed.
- Added versioned, validated, bounded JSON configuration, recording, and path storage with atomic saves and corruption reporting.

### Safety and lifecycle

- Preserved the real player's position, velocity, rotation, physics, bounding box, and networking behavior.
- Added cleanup for disconnect, world or connection replacement, respawn, player replacement, dimension change, death, spectator mode, perspective change, and shutdown.
- Added screen suspension that preserves active camera, collision, target, roll, zoom, recording, and path timelines without queued input leakage.

Free Camera 3.0.0 targets Minecraft 26.1.2 with Fabric Loader 0.18.5+, Fabric API 0.145.4+26.1.2, and Java 25.
