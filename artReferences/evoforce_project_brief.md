# Project Brief: EVOFORCE // HUD - Skill Selection Interface

## Project Overview
**EVOFORCE** is a real-time grid-based battler developed in LibGDX, drawing core mechanical inspiration from the *Mega Man Battle Network* series. The project focuses on modernizing the classic "Custom Screen" (skill selection) into a high-fidelity, "Cyber-Tactical" HUD experience.

## Design Philosophy: "Obsidian Protocol"
The visual identity, codenamed **Obsidian Protocol**, prioritizes immersion and tactical clarity.
- **Aesthetic:** High-contrast neon accents (Cyber Blue/Pulse Crimson) against deep "Obsidian" surfaces.
- **Visual Effects:** Glassmorphism, scanline overlays, glowing tile states, and isometric grid perspectives.
- **Typography:** Space Grotesk for a digital, terminal-like readout feel.

## Functional Requirements (Skill Selection)

### 1. Tactical HUD & Operator Stats
- **Resource Tracking:** Display real-time **Mana** levels instead of traditional HP during the selection phase.
- **Slot Capacity:** A dedicated tracker for the skill hand (standardized at **6 slots**).
- **Operator Readout:** Dynamic header showing "Operator_01" status and Sync Ratio.

### 2. Grid Visualization
- **Perspective:** Isometric 3D battle grid.
- **Field State:** Clear visual distinction between Player and Enemy territories via tile color-coding.
- **Tactical Overlay:** Real-time positioning of player and enemy units (e.g., Skeleton Knight) to inform skill selection.

### 3. Skill Management & Selection
- **Skill Tray:** A bottom-docked horizontal tray displaying the current "hand" of available skills (Chips).
- **Elemental Iconography:** Distinct visual markers for skill types (Wind, Fire, Ice, Elec, Wood).
- **Detail Panel:** A sidebar/panel that displays:
    - Skill Name & Icon
    - Attack Power (e.g., 80 Damage)
    - Accuracy Rating (e.g., 100%)
    - Description/Behavior (e.g., "Dash forward 1 tile and slash...")
    - Elemental Class & Mana Cost.

### 4. Queue System (Controller Optimized)
- **Mapping:** Three primary queue slots mapped to **[X]**, **[Y]**, and **[B]** controller inputs.
- **Layout:** Horizontal queue display on the right-hand side of the interface for rapid visual confirmation.
- **Action Execution:** Clear "OK" or "EXECUTE" call-to-action to lock in the sequence and return to real-time combat.

## Technical Context
- **Platform:** LibGDX (Java-based framework).
- **Resolution:** Targeted for Desktop (16:9 aspect ratio).
- **UI Architecture:** Modular components designed for responsive scaling and layering within the LibGDX Stage/Viewport system.
