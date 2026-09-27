# Goblins vs KavirLand

**Beta version: 0.0.2**

An original Android fantasy horde-survival prototype. The project uses procedural Canvas graphics and does not copy assets or source code from other games.

## 0.0.2 — Gameplay & visual upgrade
- Manual START WAVE and NEXT WAVE flow
- Richer procedural environment and character rendering
- Goblin variants: Goblin, Runner and Elite
- Combat particles, hit effects and level-up effects
- Expanded HUD with wave, HP, XP, damage, shots, kills and gold
- In-game Settings menu
- Look sensitivity, auto-next-wave and FPS options
- Improved weapon and crosshair presentation

## Version policy
The beta starts at **0.0.1**.

A version is bumped when a release has **more than 2 meaningful changes/features/fixes** since the previous version:
**0.0.1 → 0.0.2 → 0.0.3 → ...**

Small isolated fixes can remain on the current version. Every bump should be recorded in the changelog.

## Build
Pushes to `main` run the Android Debug APK workflow. The APK is uploaded as a GitHub Actions artifact and attached to the matching GitHub Release.

## Controls
- START WAVE: start the current wave
- Left joystick: move
- Drag the middle/right area: look
- Hold FIRE: attack
- NEXT WAVE: manually start the next wave
- SETTINGS: change gameplay display options
- After death: use RESTART
