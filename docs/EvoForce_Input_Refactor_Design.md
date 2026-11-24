# EvoForce: Event-Driven Input Refactor

Architecture & design specification  |  Status: Approved  |  Java / libGDX

## 1. Purpose and scope

Replace render-frame input polling with event-driven capture, ordered logical action transitions, and immutable `InputFrame` delivery. Implement this before the fixed 60 Hz simulation refactor, without changing combat timing or action legality.

### Goals

- Capture keyboard and controller press/release transitions via callbacks.
- Preserve the `GameAction` abstraction and deliver transitions in arrival order (a Y press that arrives before an X press is attempted first).
- Maintain held-state semantics for modifiers and skill loading.
- Consume exactly one immutable `InputFrame` per render-driven update; later, one per fixed simulation tick.
- One input API for every screen, backed by one app-lifetime `InputManager`.
- Prepare a clean input boundary for replay and eventual rollback.

### Out of scope

No fixed timestep yet, gameplay action buffer, combos/cancels, rollback, networking, local multiplayer, controller assignment, or input-rebinding UI.

## 2. Existing architecture and problems

Every screen (`MainMenuScreen`, `OverworldScreen`, `GameScreen`, `RewardScreen`, `GameOverScreen`) builds its own `InputManager.defaultSetup()` and polls it. `GameScreen.render(delta)` calls `input.update()`, `currentState.input()`, `currentState.update(delta)`, then render. `KeyboardInputSource` polls `Gdx.input.isKeyPressed`; `GamepadInputSource` receives callbacks but only samples its booleans in `update()`; `InputManager` compares current/previous logical states. `PlayState` checks actions in a hardcoded order (X before Y).

- A press and release between two samples is lost (both devices).
- Input order is not preserved by hardcoded action checks.
- States query a mutable `InputManager` instead of consuming explicit per-update input.
- No frame-indexed input record for testing, replay or rollback.
- Every `new GamepadInputSource()` registers a `Controllers` listener that is never removed, so each screen visit leaks one.
- A fresh per-screen manager reports a key that is already held as "just pressed" on its first update.
- `MainMenuScreen` calls `Gdx.input.setInputProcessor(stage)` / `(null)`, which would wipe any keyboard adapter registered elsewhere.

## 3. Target pipeline

```
Keyboard InputAdapter       Gamepad ControllerAdapter
          \                    /
             InputManager            (one instance, owned by Main)
       (per-device held state)
       (logical edge detection)
                    |
         FIFO InputEvent queue
                    |
   InputFrame(tick, events, heldBefore, held)
                    |
        the active screen's render
           /                \
  GameScreenState.input(frame)   menu / overworld / reward / game-over
       /           \
  PlayState   SkillSelectState
       |
  Encounter / CombatSystem
```

Input callbacks record intent only. They must never call `Encounter` or change gameplay state. The event queue transports transitions to the next update; it is NOT a buffer for rejected gameplay actions.

## 4. Core types

### InputEvent

```java
public record InputEvent(GameAction action, Type type) {
    public enum Type { PRESSED, RELEASED }
}
```

Each event is a logical transition. Duplicate physical notifications while held never produce duplicate `PRESSED` events.

### InputFrame

```java
public record InputFrame(
    long tick,
    List<InputEvent> events,
    Set<GameAction> heldBefore,
    Set<GameAction> held
) {
    public InputFrame {
        events = List.copyOf(events);
        heldBefore = Set.copyOf(heldBefore);
        held = Set.copyOf(held);
    }

    public boolean pressed(GameAction action) { ... }  // any PRESSED event for action
    public boolean isHeld(GameAction action)  { ... }  // held.contains(action)
}
```

- `events` is ordered and may be empty or hold several transitions, including a press and release of the same action.
- `heldBefore` is the logical held state before the first event; `held` is the state after the last. A consumer that needs event-time modifier state starts from `heldBefore` and applies transitions as it walks `events`.
- `tick` is a monotonically increasing update index now and the simulation tick number later. The caller owns the counter.
- `pressed` / `isHeld` serve consumers that don't care about order (menus, overworld movement).

## 5. Input sources and aggregation

`InputDevice { KEYBOARD, GAMEPAD }` distinguishes the two held sets; it is not multiplayer routing. `GameAction` and the existing bindings are unchanged. Sources report straight to the manager through `InputManager.set(device, action, pressed)`; there is no separate sink interface.

### Keyboard

`KeyboardInputSource extends InputAdapter`. `keyDown` / `keyUp` map the keycode to a `GameAction` and call `set(KEYBOARD, action, …)`; unmapped keys are ignored and both callbacks return `false` so Scene2D stages still see the event. LWJGL3 does not deliver OS key repeat as `keyDown`, and the manager's dedupe covers it regardless.

### Gamepad

`GamepadInputSource extends ControllerAdapter` keeps its raw booleans, deadzones and trigger mappings. After every callback (`buttonDown`, `buttonUp`, `axisMoved`, `disconnected`) it publishes each action's aggregated value (`dpadUp || stickUp`, …) through `set(GAMEPAD, action, …)`. The manager ignores unchanged values, so only real transitions become events, and releasing the D-pad does not release an action the stick still holds. On disconnect the raw booleans are cleared and published, which releases gamepad-held actions while keyboard-held ones stay held.

### InputManager

```java
EnumMap<InputDevice, EnumSet<GameAction>> deviceHeld;
EnumSet<GameAction> logicalHeld;
Set<GameAction> heldAtLastFrame;
List<InputEvent> pendingEvents;   // ArrayList: append + clear only

void set(InputDevice device, GameAction action, boolean pressed);
InputFrame nextFrame(long tick);
boolean isHeld(GameAction action);   // render-side reads, e.g. LoadedHud
```

A logical action is held if any device holds it. `set` updates the device's set, returns if nothing changed, and enqueues `PRESSED` only on logical false→true and `RELEASED` only on true→false, in arrival order. `nextFrame` snapshots the queue with `heldAtLastFrame` and the current `logicalHeld`, clears the queue, and remembers `held` as the next frame's `heldBefore`.

Threading: on LWJGL3 both GLFW key callbacks and gdx-controllers polling run on the main thread before `render`, so the queue is a plain `ArrayList` with no marshalling.

### Ownership and routing

`Main` owns the single `InputManager` and an `InputMultiplexer`, both created in `create()` and alive for the whole app. The keyboard source is registered in the multiplexer and the gamepad source with `Controllers.addListener` exactly once. Screens that need a Scene2D stage to receive input add it to the multiplexer in `show()` and remove it in `hide()`; nothing calls `Gdx.input.setInputProcessor` after startup.

## 6. Game loop and state integration

### Before fixed timestep

```java
InputFrame frame = game.input.nextFrame(inputTick++);
currentState.input(frame);
currentState.update(delta);
currentState.render(game.batch);
```

`GameScreenState.input()` becomes `input(InputFrame frame)`. The variable-delta update and rendering stay intact. Exactly one frame is consumed per render, by whichever screen is active; the other screens call `nextFrame` once at the top of their own input handling.

### After fixed timestep (future, not this PR)

```java
while (accumulator >= FIXED_DT) {
    InputFrame frame = input.nextFrame(simulationTick++);
    playState.input(frame);
    playState.fixedUpdate(FIXED_DT);
    accumulator -= FIXED_DT;
}
```

When rendering faster than simulation, events stay queued until the next tick. During catch-up each batch is consumed once. Queued events are assigned to the next eligible tick; timestamp-to-tick reconstruction is deferred.

## 7. PlayState input semantics

Walk `frame.events()` in order:

- `MOVE_*` `PRESSED` attempts a grid step.
- `ATTACK_BASIC` `PRESSED` attempts the basic attack.
- `SKILL_X/Y/B` `PRESSED` attempts the matching slot, loading instead of casting if the right trigger is held at that moment.
- `TRIGGER_RIGHT` transitions update the local "load held" flag, seeded from `frame.heldBefore()`. It is the only modifier, so no general cursor is needed.

```
TRIGGER_RIGHT PRESSED
SKILL_Y PRESSED       // loads: trigger held
TRIGGER_RIGHT RELEASED
SKILL_X PRESSED       // casts: trigger released
```

After the loop, two checks run against `frame.held()` and stay level-triggered, exactly as today:

- **Loaded-skill release**: right trigger not held and skills are loaded → release the burst.
- **Staging menu open**: both triggers held and `mana.canOpenMenu()` → switch to `SkillSelectState`. Being a level check is what lets the player hold both triggers while mana fills and have the menu open the moment it crosses half.

Order of attempted actions is guaranteed; whether an attempt succeeds is still decided by existing combat rules (eligibility, mana, slot popping, loading, casting). No automatic retry or action buffering.

## 8. SkillSelectState and transitions

`SkillSelectState` walks `PRESSED` events in order for slot assignment, confirm, cancel, undo, tuck and navigation, keeping its existing rules: tuck toggles and everything else is ignored while tucked; the trait tray is modal; processing stops as soon as the state starts exiting. The overlay's render-driven timing and pause behaviour are unchanged.

Transition policy: the state or screen that takes the frame owns it. Once it begins exiting or changes state, remaining events are dropped, never replayed into the incoming state. Held state lives in the app-lifetime manager, so it survives every state and screen transition, and a button already held never produces a fresh press. This is what stops gamepad A (bound to both `SKILL_SELECT_CONFIRM` and `ATTACK_BASIC`) from confirming the menu and then firing a basic attack.

## 9. Lifecycle and cleanup

- **Focus loss**: no special handling. GLFW synthesises key releases when the window loses focus, so keyboard-held state clears itself through the normal path. The visible result matches today: alt-tabbing with the load trigger held releases the loaded skills.
- **Gamepad disconnect**: handled by the gamepad source as described in §5.
- **Listeners**: registered once in `Main.create()` for the life of the app; screens only add/remove their own stage on the multiplexer.

## 10. Test plan

`InputManager` is pure Java, so it is unit-tested with JUnit 5 (`core/src/test`, no libGDX backend needed). The remaining rows are checked by hand in the running game.

| Area | Required checks | How |
| --- | --- | --- |
| Transitions | One press/one release; duplicate `set` ignored; press+release between updates both retained; held persists through empty frames; queue empty after `nextFrame`; `tick` passed through. | Unit |
| Ordering | Y→X and X→Y retained; press/release/press order; no event delivered twice. | Unit |
| Aggregation | Keyboard+gamepad on the same action gives one `PRESSED` and a `RELEASED` only when both let go; `heldBefore`/`held` correct across frames. | Unit |
| Aggregation (device) | D-pad+stick; trigger threshold crossings; disconnect. | Manual |
| Gameplay | Movement, basic attack, skill loading/casting, loaded-skill release, menu opens when mana reaches half with both triggers held, all staging-menu controls; gamepad A confirms without a follow-up basic attack. | Manual |
| Screens | Main menu keys and mouse; overworld held movement; reward and game-over do not auto-advance on a held confirm. | Manual |
| Lifecycle | Alt-tab with trigger held, state and screen transitions; no stuck or replayed inputs. | Manual |

## 11. Files and boundaries

New: `input/InputEvent.java`, `InputFrame.java`, `InputDevice.java`; `core/src/test/java/com/silverignis/input/InputManagerTest.java`.

Rewritten: `input/InputManager.java`, `KeyboardInputSource.java`, `GamepadInputSource.java`.

Deleted: `input/InputSource.java`.

Changed: `Main.java` (owns manager + multiplexer); `screens/GameScreen.java`, `MainMenuScreen.java`, `OverworldScreen.java`, `RewardScreen.java`, `GameOverScreen.java`; `screens/state/GameScreenState.java`, `PlayState.java`, `SkillSelectState.java`; `core/build.gradle` (JUnit, test-only); `SPEC.md`.

Unchanged: `GameAction`, `Encounter`, `CombatSystem`, `SkillInstance`, `MovementSystem`, mana/cooldown systems, enemy AI, rendering and skill phase logic.

## 12. Acceptance criteria and future extensions

- All keyboard/gamepad transitions originate from callbacks; nothing polls `Gdx.input` for gameplay or menu actions.
- One `InputManager` exists for the life of the app and emits ordered logical events, one immutable `InputFrame` per update.
- Every screen and state receives input as an `InputFrame`; arrival order is preserved.
- Slot load-vs-cast uses event-time trigger state; menu-open and loaded-skill release remain level-triggered.
- Focus loss, disconnects, and state or screen changes cannot leave stuck or replayed actions.
- Moving `nextFrame()` into a future 60 Hz loop requires no redesign of device adapters or the event queue.

Intentional behaviour changes (everything else is preserved):

- Presses are handled in arrival order instead of a hardcoded X, Y, B order.
- A press and release inside one frame is no longer lost.
- A key held across a screen change no longer counts as a fresh press on the new screen.
- In the staging menu, whichever of confirm/cancel arrives first wins (previously confirm always won).

Later additions: fixed 60 Hz simulation, optional gameplay action buffering, input recording, network input streams, deterministic state snapshots, rollback and resimulation. These are separate projects.

## 13. Architectural invariant

Every update consumes exactly one immutable `InputFrame` containing the ordered logical input events assigned to that update, plus the held-action state before and after them. Input capture records player intent; gameplay determines whether that intent can execute.
