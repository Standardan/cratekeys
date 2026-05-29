# CrateKeys

Reward **crates** with collectible keys and an **animated roulette** opening, for Paper 1.21+.
Bind any block as a crate, hand out keys, and let players right-click to spin for weighted rewards.

## Download

**[Download the latest release »](https://github.com/Standardan/cratekeys/releases/latest)**

Drop the `.jar` into your server's `plugins/` folder and restart. Requires Paper 1.21+ (Java 21).

## Features

- **Config-driven crates** — define crates, key items, and weighted reward tables in `config.yml`
- **Animated spin GUI** — icons scroll across a roulette and land on the winning reward, with sound
- **Tagged key items** — keys are identified by a PersistentDataContainer tag, not their name
- **Block-bound crates** — `/crate setblock <crate>` turns any block into a crate; right-click with a key opens it
- **Weighted rewards** run as console commands (`give`, `eco`, anything), so rewards are unlimited
- The spin GUI is **click-locked** so players can't pull items out mid-animation

## Commands (`cratekeys.admin`, default op)

| Command | Description |
|---|---|
| `/crate givekey <player> <crate> [amount]` | Give crate keys |
| `/crate setblock <crate>` | Bind the block you're looking at as a crate |
| `/crate removeblock` | Unbind a crate block |
| `/crate reload` | Reload config |
| `/crate list` | List defined crates |

## Configuration

```yaml
crates:
  vote:
    display-name: "<aqua><bold>Vote Crate</bold>"
    key-name: "<aqua>Vote Key"
    key-material: TRIPWIRE_HOOK
    rewards:
      - name: "<white>16 Diamonds"
        weight: 50        # relative odds
        icon: DIAMOND
        commands:
          - "give %player% diamond 16"
```

## Design notes

- **The winner is decided before the animation starts** (`Crate#roll()` does a single weighted draw).
  The roulette is pure presentation — it can't change the outcome, which keeps rewards honest and
  the code simple.
- **The GUI is identified by a custom `InventoryHolder`** (`SpinGui`), so the click listener cancels
  interaction by *type*, never by fragile title-string matching.
- **Animation runs on a repeating scheduler task** at a fixed tick interval and cancels itself when done.

## Building

JDK 21 + Maven. `mvn clean package` → `target/cratekeys-1.0.0.jar`.
