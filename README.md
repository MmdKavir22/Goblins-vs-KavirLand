# Goblins vs KavirLand

**Beta version: 0.0.1**

An original Android fantasy horde-survival prototype. The project uses procedural Canvas graphics and does not copy assets or source code from other games.

## 0.0.1 — First playable beta
- Landscape Android build
- Touch joystick movement
- Swipe/drag look control
- Hold FIRE to attack
- Increasing goblin waves
- HP, XP, levels, kills and gold
- Death/restart flow
- Automatic Debug APK build with GitHub Actions

## Version policy
The beta starts at **0.0.1**.

A version is bumped when a release has **more than 2 meaningful changes/features/fixes** since the previous version:
**0.0.1 → 0.0.2 → 0.0.3 → ...**

Small isolated fixes can remain on the current version. Every bump should be recorded in the changelog.

## Build
Pushes to `main` run the Android Debug APK workflow. The APK is uploaded as a GitHub Actions artifact.

## Controls
- Left joystick: move
- Drag the middle/right area: look
- Hold FIRE: attack
- After death: tap anywhere to restart
