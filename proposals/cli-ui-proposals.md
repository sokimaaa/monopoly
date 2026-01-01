# CLI UI Proposals (Monopoly)

I reviewed the current CLI flow in `mnpl-infra-cli/src/main/scala/infrastructure/adapter/cli/ConsoleGameController.scala`. Below are UI proposals focused on a cleaner, dynamic map, emoji/color usage, and button-like actions with state-dependent enable/disable behavior. I used Context7 docs for Lanterna (the only terminal UI lib available there).

## Option A — Lanterna GUI (full TUI with widgets + buttons)
Use Lanterna’s GUI components to create a real terminal UI: map panel, stats panel, and an action bar with buttons that can be enabled/disabled based on game state. Dynamic redraw happens inside the terminal buffer, reducing console spam.

Pros
- Real buttons, layouts, dialogs; clean separation of map vs actions
- Supports colors, symbols, emoji out of the box
- Smooth dynamic updates with `screen.refresh()` (no log spam)

Cons
- More moving parts (screen lifecycle, GUI event loop)
- Slightly heavier dependency + learning curve

Snippet (Scala, using Lanterna GUI APIs)
```scala
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.gui2._
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory

val terminal = new DefaultTerminalFactory().createTerminal()
val screen = new TerminalScreen(terminal)
screen.startScreen()

val gui = new MultiWindowTextGUI(screen)
val window = new BasicWindow("Monopoly")

val root = new Panel()
root.setLayoutManager(new LinearLayout(Direction.VERTICAL))

val header = new Label("🎩 Monopoly — Turn UI").setForegroundColor(TextColor.ANSI.YELLOW)
root.addComponent(header)

val actions = new Panel(new GridLayout(4))
val rollBtn = new Button("🎲 Roll", () => rollDice())
val buyBtn  = new Button("🏠 Buy", () => buyProperty())
val endBtn  = new Button("⏭ End", () => endTurn())
val quitBtn = new Button("🚪 Quit", () => quit())

actions.addComponent(rollBtn)
actions.addComponent(buyBtn)
actions.addComponent(endBtn)
actions.addComponent(quitBtn)

root.addComponent(actions)
window.setComponent(root)
gui.addWindowAndWait(window)
```

Dynamic map render (Lanterna Screen + TextGraphics)
```scala
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.graphics.TextGraphics

val tg: TextGraphics = screen.newTextGraphics()
tg.setForegroundColor(TextColor.ANSI.GREEN)
tg.putString(2, 2, "🧭 Board")
tg.putString(2, 3, "GO  |  A1 |  A2 |  Jail")
screen.refresh()
```

Enabling/disabling buttons by state (concept)
```scala
rollBtn.setEnabled(!turnActive)
buyBtn.setEnabled(turnActive && canBuy)
endBtn.setEnabled(turnActive || currentPlayer.isBankrupt)
```

## Option B — Lanterna Screen + custom renderer (no GUI widgets)
Use the `Screen` API to draw a custom layout and read input events. You keep full control over the map rendering and use ANSI colors/emoji for clarity, but you build your own "button bar" UI.

Pros
- Tight control over visuals and layout (great for a Monopoly board)
- Minimal overhead compared to full GUI widgets
- Still avoids console spam by redrawing the screen buffer

Cons
- You manage layout, focus, and input handling manually
- No built-in buttons/dialogs; must implement state and highlighting

Snippet (Scala, using Lanterna Screen/TextGraphics)
```scala
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory

val screen: Screen = new DefaultTerminalFactory().createScreen()
screen.startScreen()

val tg = screen.newTextGraphics()
tg.setForegroundColor(TextColor.ANSI.WHITE)
tg.putString(1, 1, "🎲 Roll  🏠 Buy  🔨 Decline  ⏭ End  📊 Status  🚪 Quit")
tg.setForegroundColor(TextColor.ANSI.CYAN)
tg.putString(1, 3, "🧭 Board: GO | A1 | A2 | Jail | ...")
screen.refresh()
```

## How this fits the current architecture
- Keep all logic in application use cases unchanged.
- Add a new CLI adapter alongside `ConsoleGameController`, e.g. `TuiGameController` in `mnpl-infra-cli`, responsible only for rendering + input.
- The TUI pulls game state from existing ports (`GameRepository`) and triggers existing use cases.
- The map view is a pure rendering concern: build a `BoardRenderer` from `domain.Board` and `domain.Square`.

## Recommended path
- If you want a polished, button-driven UI with minimal custom wiring: go with Option A (Lanterna GUI).
- If you want a "game-like" board view with total control and less widget overhead: go with Option B (Lanterna Screen).

If you want, I can draft a concrete TUI layout (map + sidebar + actions) and add it as a new adapter in `mnpl-infra-cli` while keeping the current console controller intact.

## UI difference screens

Lanterna GUI (widgets/buttons)
```
┌─────────────────────────────────────────────────────────────────────────┐
│ 🎩 Monopoly — Turn UI                                                   │
├──────────────────────────────────────────────┬──────────────────────────┤
│ 🧭 Board                                     │ 👥 Players               │
│ GO | A1 | A2 | Jail | A3 | ...               │ Alice  $1200  🟢          │
│                                              │ Bob    $ 850  🔴 (Jail)   │
│                                              │ Cara   $1400  🟢          │
├──────────────────────────────────────────────┴──────────────────────────┤
│ [🎲 Roll] [🏠 Buy] [🔨 Decline] [⏭ End] [📊 Status] [🚪 Quit]             │
└─────────────────────────────────────────────────────────────────────────┘
```

Lanterna Screen (no GUI widgets, custom renderer)
```
MONOPOLY — Turn UI
───────────────────────────────────────────────────────────────────────────
🧭 Board
GO | A1 | A2 | Jail | A3 | ...

👥 Players
Alice  $1200  🟢
Bob    $ 850  🔴 (Jail)
Cara   $1400  🟢

Actions: 🎲 Roll  🏠 Buy  🔨 Decline  ⏭ End  📊 Status  🚪 Quit
Hint: Press [r/b/d/e/s/q], highlight active options in color.
```

## Full board screen (proposed)
```
┌──────────────────────────────────────────────────────────────────────────────┐
│ 🎩 MONOPOLY — Turn UI                                                        │
├───────┬────────┬────────┬────────┬────────┬────────┬────────┬────────┬────────┤
│ GO 🟢 │ Med Av │ Balt Av│ IN TX  │ Read RR│ Orient│ Chance │ Vermnt  │ Conn Av│
│ P1 🎩 │        │        │        │        │       │        │         │        │
├───────┼────────┴────────┴────────┴────────┴────────┴────────┴────────┴────────┤
│ St Ch │                                                             │ Jail 🔒 │
│       │                                                             │ P2 🚗   │
├───────┤                                                             ├─────────┤
│ Statl │                                                             │ St.James│
│       │                                                             │         │
├───────┤                                                             ├─────────┤
│ VA Av │                                                             │  Comm Ch│
│       │                                                             │         │
├───────┤                                                             ├─────────┤
│Penn RR│                                                             │  Tenn Av│
│       │                                                             │         │
├───────┤                                                             ├─────────┤
│St.Jam │                                                             │  NY Av  │
│       │                                                             │         │
├───────┤                                                             ├─────────┤
│ Chance│                                                             │  KY Av  │
│       │                                                             │         │
├───────┤                                                             ├─────────┤
│ IL Av │                                                             │   Chance│
│       │                                                             │         │
├───────┤                                                             ├─────────┤
│ B&O RR│                                                             │  IN Av  │
│       │                                                             │         │
├───────┤                                                             ├─────────┤
│ Atl Av│                                                             │  CT Av  │
│       │                                                             │         │
├───────┤                                                             ├─────────┤
│ Ventn │                                                             │   Water │
│       │                                                             │   Works │
├───────┤                                                             ├─────────┤
│ Com Ch│                                                             │  Marvin │
│       │                                                             │  Gardens│
├───────┤                                                             ├─────────┤
│ GO TO │                                                             │  Pacific│
│ JAIL  │                                                             │         │
├───────┤                                                             ├─────────┤
│ Free  │                                                             │ NC Av   │
│ Park  │                                                             │         │
├───────┴────────┬────────┬────────┬────────┬────────┬────────┬────────┬────────┤
│ RR   🟢        │ Chance │ PA Av  │ Short  │ IN Tax │ Park   │ Lux Tax│ Board  │
│                │        │        │ Line RR│        │ Place  │        │ Walk   │
└────────────────┴────────┴────────┴────────┴────────┴────────┴────────┴────────┘

Players: P1 🎩 (Alice) $1200 | P2 🚗 (Bob) $850 (Jail) | P3 🐶 (Cara) $1400
Turn:    P1 🎩 Position: GO  Balance: $1200  Dice: 2+5=7
Actions: [🎲 Roll] [🏠 Buy] [🔨 Decline] [⏭ End] [📊 Status] [🚪 Quit]
Legend:  🟢 current player | 🔒 in jail
```
