# Attribute Dice

A Minecraft Java 1.21.11 Fabric mod where players use rare materials to craft Attribute Dice and enhance their attributes! Roll high to gain stats (damage / armor / max health); roll low to lose them. Beware rolling a 1 — lightning strikes!

> Tip: Too much gambling can hurt you, so don't overuse it!

## Features

- **Crafting recipe** (3x3): 4 corners = Gold Block, center = Netherite Block, remaining 4 slots = Diamond.
- **Right-click** the dice item to throw a spinning dice entity in front of you. After ~3 seconds it stops and shows the rolled face.
- **4 / 5 / 6 (high)**: gain a random attribute (Attack Damage / Armor / Max Health), value in `[gainMin, gainMax]` (default `[1, 10]`).
- **1 / 2 / 3 (low)**: lose a random attribute, value in `[lossMin, lossMax]` (default `[1, 5]`).
- **Rolling a 1**: also deals `lightningDamage` lightning damage (default 10) and strikes the player with a lightning bolt.
- Each face (1–6) is equally likely.
- Chat message shows the roll value: red for 1/2/3, green for 4/5/6.

All of the above is configurable via `config/attribute_dice.json` (auto-created on first run).

## Build

Requirements: **JDK 21** and internet access (Gradle will download Minecraft mappings).

```bat
gradlew.bat build
```

Output jar: `build/libs/attribute-dice-1.0.0.jar` — drop it into your Fabric `mods/` folder together with Fabric API.

## Texture placement

The mod does NOT ship any texture by default — please drop your own PNGs into the
following locations under `src/main/resources/assets/attribute_dice/textures/`:

| Purpose        | Required path                                                  | Recommended size |
|----------------|----------------------------------------------------------------|-------------------|
| Item icon      | `item/attribute_dice.png`                                      | 16x16             |
| Entity texture | `entity/dice.png` (one texture used on all 6 faces of the cube) | 16x16             |

Directory tree example:

```
src/main/resources/assets/attribute_dice/
├── lang/
│   ├── en_us.json
│   └── zh_cn.json
├── models/
│   └── item/
│       └── attribute_dice.json
└── textures/
    ├── item/
    │   └── attribute_dice.png   <-- your item icon goes here
    └── entity/
        └── dice.png             <-- your cube face texture goes here
```

After dropping the textures in, re-run `gradlew.bat build` to package them into the jar.
