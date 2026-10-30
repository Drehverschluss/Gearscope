<p align="center">
  <img src="src/main/resources/gearscope.png" alt="Gearscope" width="160">
</p>

<h1 align="center">Gearscope</h1>

<p align="center">
  <img alt="Minecraft 1.21.1" src="https://img.shields.io/badge/Minecraft-1.21.1-5b8731">
  <img alt="NeoForge" src="https://img.shields.io/badge/NeoForge-21.1-e68c37">
  <img alt="License MIT" src="https://img.shields.io/badge/License-MIT-blue">
</p>

🛡️ **See your whole hero at a glance.** Gearscope is a NeoForge mod for Minecraft 1.21.1 (Java 21) that adds a
classic RPG character screen next to the vanilla inventory: your character on a pedestal, armor and accessories
placed where they belong, your full inventory with 2×2 crafting, and every attribute you have, grouped on a
parchment scroll and updated live while you change your gear.

> **Client & Server required.** Gearscope is a real container screen, so it has to be installed on the server and
> on every client.
>
> **Key binding unbound by default.** Assign *Open Gearscope* under *Options → Controls → Gearscope*; the same key
> closes Gearscope again. The vanilla inventory on **E** stays exactly as it is; Gearscope is an alternative, not a
> replacement. To put Gearscope on **E**, move the vanilla inventory to another key (e.g. Shift+E) and turn on
> `closeContainersWithKey` so E still closes chests and other containers.

## Screenshots

<!-- Screenshots will be added later. -->

## How it works

Pressing the key sends the `gearscope:open_gearscope` payload to the server, which opens the Gearscope menu
(`gearscope:character`). The menu contains the vanilla slots (2×2 crafting, armor, offhand, inventory, hotbar) and,
with Accessories installed, one functional and one cosmetic slot for every accessory slot the player has. The server
sends the list of accessory slots along with the menu, so client and server always build the same slots. If the
player's accessory slots change while the screen is open (e.g. an item that adds a ring slot), the server reopens
the menu and keeps the item on the cursor and the crafting grid.

Everything about the look (positions, which slot goes where, attribute groups) is decided on the client from
resource files, so modpacks can rearrange the screen with a resource pack.

Cosmetic armor uses Accessories classes that are not part of its official API. If an Accessories update changes
them, Gearscope turns cosmetic armor off and logs it once instead of crashing.

## Installation

1. Install NeoForge `21.1` for Minecraft `1.21.1`.
2. Put `gearscope-0.2.0.jar` into the `mods` folder of the **server and of every client**.
3. Bind *Open Gearscope* in the controls.
4. Optional: install any of the mods below; Gearscope picks them up automatically.

| Mod | Tested version | Needed |
|---|---|---|
| Accessories | `1.1.0-beta.54+1.21.1` | optional (accessory slots, cosmetic armor) |
| Apothic Attributes | `1.21.1-2.11.0` | optional (detailed attribute tooltip, hidden attributes) |
| JEI | `19.70.0.462` | optional |
| Polymorph | `1.2.0+1.21.1` | optional |
| Sophisticated Backpacks | `3.26.17` | optional |
| Mouse Tweaks | `2.26.1` | optional |
| Inventory Sorter | `24.0.24` | optional |

All integrations are `compileOnly` at build time and only loaded when the mod is installed. Without them, the
corresponding feature is simply not there.

## What it does

### The character screen

- **Your real character** stands on a pedestal in the middle and follows the mouse.
- **Armor and offhand** sit in the wooden strip on the left.
- **Accessories** are placed around the character where they belong, in four rows with an inner and an outer
  column on each side: necklace and cape at the shoulders (the outer shoulder places are free for packs), rings and
  hand slots at the hands, belts and charms at the waist, shoes and anklets at the feet (two-slot types get one place
  on each side). A strip on the right holds hat, face and wrist, and the back slot sits at the bottom of the left
  strip.
- **Everything else** goes into group tabs next to the inventory, in Accessories' group order. Slots without a group
  land in an "Other" tab, so nothing gets lost.
- **Full inventory, hotbar and 2×2 crafting**: shift-click, double-click and hotbar keys work as usual.
  Shift-click puts accessories into the first free matching slot.
- **Cosmetic mode**: one button switches all accessory slots (and the armor slots, as cosmetic armor) to their
  cosmetic variants. Cosmetic slots get a violet frame.
- **Show on character**: like in Accessories' own screen, every accessory slot has a small switch at its top right
  corner that shows or hides the item on the character (the item stays equipped).

### Attributes on a scroll

- **Every attribute the player has**, grouped into Offense, Ranged, Defense, Magic, Movement, Utility and Other.
- **Live values**: better than base is green, worse is red, using the attribute's own sentiment (so "damage taken"
  going up is red).
- **Collapsible groups** and a **"hide unchanged"** checkbox, both saved in the client config.
- **Attribute icons** from resource packs are picked up automatically.
- Hovering a line shows a tooltip with description, current and base value and all modifiers. With **Apothic
  Attributes** installed it is Apothic's detailed tooltip (base/min/max, modifier sources, formula), and attributes
  hidden in Apothic's config are hidden in Gearscope too.

### Active effects

Active effects (potions, buffs) are shown with icon, name with level, remaining time (∞ for infinite effects) and
a tooltip, green for beneficial and red for harmful effects; beacon effects are marked like in the vanilla HUD. The
same effects as in the vanilla inventory are listed, in the same order. If a resource pack provides a
`<effect translation key>.desc` entry, the tooltip shows it as description. Display modes (client config
`[effects] display`):

- `SCROLL_TAB` (default): "Attributes" and "Effects" tabs at the top of the scroll; the Effects tab shows the number
  of active effects. The selected tab is remembered while the game runs.
- `SCROLL_GROUP`: an "Effects" group above the attribute groups, collapsible like the others ("hide unchanged" does
  not apply to it).
- `TOP`: a row of tiles centered above the frame, wrapping into more rows (growing upwards). If there is not enough
  room above the frame (small window, automatic GUI scaling), `SCROLL_TAB` is used instead. JEI keeps its overlays
  off the tiles.
- `OFF`: no effect display.

### Small windows

Gearscope needs a 480×270 GUI area. If that does not fit at the current GUI scale (e.g. GUI scale 4 in a 1080p
window), the GUI scale is lowered while the screen is open and restored afterwards. The saved GUI scale option is
never changed.

## Configuration

Client config `config/gearscope-client.toml`:

| Section | Option | Meaning |
|---|---|---|
| `screen` | `autoScale` | Temporarily lower the GUI scale if the screen does not fit (default `true`) |
| `screen` | `closeContainersWithKey` | The Gearscope key also closes other container screens (chests, furnaces, backpacks ...), like the vanilla inventory key; not while a text field has focus and not in the creative inventory. Useful if Gearscope is bound to E and the vanilla inventory to another key (default `false`) |
| `effects` | `display` | Where active effects are shown: `SCROLL_TAB` (default), `SCROLL_GROUP`, `TOP` (falls back to `SCROLL_TAB` without room above the frame) or `OFF`; applies right away |
| `attributes` | `hideUnchanged` | Only show attributes whose value differs from the base value (also the checkbox on the scroll) |
| `attributes` | `collapsedGroups` | Collapsed attribute groups (group ids, also set by clicking a group header) |
| `attributes` | `hiddenAttributes` | Attributes that are never shown: `minecraft:generic.luck`, `some_mod:*`, `!some_mod:visible_one`. With Apothic Attributes, its "Hidden Attributes" list applies in addition |

Common config `config/gearscope-common.toml` (read on the server, where the slots are built):

| Section | Option | Meaning |
|---|---|---|
| `accessories` | `ignoreWildcardItemsForUsedSlots` | Items that fit into every slot type (`accessories:any`, e.g. Curios items tagged `curios:curio`) do not make a slot type count as used, so slot types without a real item stay hidden (default `true`). `false` uses exactly Accessories' own check |

## Data-driven customization

Everything below can be changed with a resource pack; F3+T reloads it (an open Gearscope screen is closed so it is
rebuilt with the new values).

| File | Content |
|---|---|
| `assets/gearscope/gearscope/layout.json` | Position of every area, slot and button. Missing entries keep their default. Full reference: [docs/layout.md](docs/layout.md) |
| `assets/gearscope/gearscope/slot_places.json` | Which Accessories slot type goes to which named place, e.g. `"ring": ["body_left_mid", "body_right_mid"]`. Slots without a free place go to the tabs. All place names and positions (four body rows: `*_top`, `*_mid`, `*_waist`, `*_low`, each with `*_outer_*` columns) are listed in [docs/layout.md](docs/layout.md) |
| `assets/<namespace>/gearscope/attribute_groups.json` | Which attribute belongs to which group, e.g. `{ "groups": [ { "id": "offense", "order": 10, "attributes": ["minecraft:generic.attack_damage", "some_mod:*"] } ] }`. Files from all namespaces and packs are merged, so a pack only adds its own entries |

The background, slot frames and tab icon are normal textures under `assets/gearscope/textures/gui/`.

## Mod compatibility

- **Accessories**: all slot types are read at runtime, so it works with every Accessorify slot mode and with Curios
  slots through the Accessories Compatibility Layer. Like Accessories' own screen, slot types that no item can go
  into are hidden unless "Show Unused Slots" is enabled in Accessories; a slot type that holds an item is always
  shown.
- **Apothic Attributes**: detailed attribute tooltip and hidden attributes, see above.
- **JEI**: click the crafting arrow to see the crafting recipes; the **+** button moves recipes that fit into 2×2
  straight into the grid. JEI's item list stays clear of the screen.
- **Polymorph**: the recipe selection button appears next to the crafting result (`recipe_button` in
  `layout.json`).
- **Sophisticated Backpacks**: right-click a backpack in any Gearscope slot (inventory, hotbar, armor, offhand or an
  accessory slot such as the back slot) with an empty cursor, or use Sophisticated Backpacks' own key, to open it.
  Closing the backpack brings you back to Gearscope.
- **Mouse Tweaks**: dragging works, the scroll-wheel tweak is turned off in Gearscope (it does not get along with
  accessory slots).
- **Inventory Sorter**: middle-click sorts the inventory; armor, offhand, crafting and accessory slots are left
  alone.
- **Quark**: armor swapping and putting items into shulker boxes with a right click work. Quark's sorting buttons are
  not added, and opening shulker boxes with a right click only works in the vanilla inventory (Quark's own choice).

## Project layout

| Package | Content |
|---|---|
| `com.drehverschluss.gearscope` | `Gearscope` (common entry point) and `GearscopeClient` (client entry point) |
| `core` | Mod independent model: layout, slot specs and placement, service interfaces |
| `menu` | `GearscopeMenu`, its slot classes, menu type and server-side opening/reopening |
| `network` | Payloads and their registration |
| `client` | Screen, key binding, config, GUI scale override, layout and slot place loaders, style |
| `client.attributes` | Attribute list, groups loader, icon handling, fallback tooltip |
| `compat.*` | Optional integrations (Accessories, Apothic Attributes, JEI, Polymorph, Sophisticated Backpacks, Inventory Sorter); only loaded when the mod is installed |

## Development

Requires Java 21.

- The optional mods are compiled against from the Modrinth Maven (`maven.modrinth:*`), the BlameJared Maven (JEI
  API) and the Sinytra Maven (Forgified Fabric API base, needed by the Accessories API). No local jars are needed to
  build.
- For the dev runs, every jar in `libs/test/` (git-ignored) is added via `localRuntime`, so `runClient` and
  `runServer` behave like the target modpack. Put the mod jars you want to test with there.

```
./gradlew build        # jar in build/libs
./gradlew runClient    # dev client (game directory run/)
./gradlew runServer    # dev dedicated server (game directory run-server/, accept the EULA on first start)
```

### Testing

1. `runClient`, bind *Open Gearscope*, open the screen: character on the pedestal, armor/offhand left, accessories
   around the character and in the right strip, other slots in the tabs. Move items, shift-click, use the crafting
   grid.
2. Toggle cosmetic mode; cosmetic slots get a violet frame and only change the look.
3. Equip items with attribute modifiers and watch the values change; try the "hide unchanged" checkbox and
   collapsing groups, then restart and check that both are kept.
4. Set GUI scale 4 in a 1080p window: the screen still fits and the GUI scale is back to 4 after closing it.
5. Edit `layout.json` in a resource pack (e.g. move `inventory.x` by 10) and press F3+T.
6. With the optional mods in `libs/test/`: JEI arrow and **+**, Polymorph button, opening a backpack and returning,
   middle-click sorting.
7. Remove the optional jars from `libs/test/`: client and server still start and the screen works.
8. `runServer` and connect with `runClient` (multiplayer, `localhost`).

## License and credits

MIT, see [LICENSE](LICENSE). Parts of the code are adapted from other MIT-licensed mods; their copyright notices and
license texts are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) (also included in the jar).

- The attribute tooltip and value colors are adapted from
  [Apothic Attributes](https://github.com/Shadows-of-Fire/Apothic-Attributes) by Shadows_of_Fire (MIT).
- The accessory slots are built on the [Accessories](https://github.com/wisp-forest/accessories) API by Wisp Forest
  (MIT); the cosmetic slot handling follows Accessories' own slots.
- Inspired by **Character Not Container** by CappleApple (idea only, no code used).
- Gearscope is an unofficial companion and not affiliated with any of these mods.
- Bug reports and suggestions: [issue tracker](https://github.com/Drehverschluss/Gearscope/issues).
