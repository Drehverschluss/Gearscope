# Gearscope screen layout (`layout.json`)

All positions of the Gearscope screen are read from
`assets/gearscope/gearscope/layout.json`. The mod ships this file with the default
layout; resource packs can override it like any other asset.

## Rules

- All coordinates are GUI pixels relative to the top-left corner of the screen panel
  (default panel size 480x270). Slot coordinates are the top-left corner of the 16x16 item area.
- Every section and every key is optional. Missing entries keep the built-in default,
  so a pack only needs to list the values it changes.
- Invalid entries (wrong type, negative values, wrong array length) are logged as a warning
  with the entry path (e.g. `player.scale`) and the default is used instead.
- Reloading resources (F3+T) re-reads the file. An open Gearscope screen is closed so
  that the menu is rebuilt with the new positions on the next open.
- Slot places defined under `places` can be referenced by name in `slot_places.json`.
  A pack can move existing places or add new ones; unknown place names in
  `slot_places.json` are skipped with a single warning.
- A place that no slot uses is not drawn at all.

## Default places

| Place | Position |
|---|---|
| `body_left_top`, `body_right_top` | Next to the shoulders (necklace, cape) |
| `body_left_mid`, `body_right_mid` | Next to the hands (ring) |
| `body_left_outer_mid`, `body_right_outer_mid` | Outer column at hand height (hand) |
| `body_left_low`, `body_right_low` | Next to the feet (shoes) |
| `body_left_outer_low`, `body_right_outer_low` | Outer column at foot height (anklet) |
| `strip_left_6` | Bottom of the left strip, below the offhand (back) |
| `strip_right_1` … `strip_right_6` | Right strip from top to bottom (hat, face, wrist, belt, charm; `strip_right_6` is free) |

Slot types with two slots take their places in list order, e.g. `"shoes": ["body_left_low", "body_right_low"]`.
Shift-click fills accessory slots in the order of `places` (body places first, then the strips, then the tabs).

## Example: move the inventory 10 px to the right

```json
{
  "inventory": { "x": 87 }
}
```

## Sections and keys

| Section | Key | Type | Meaning |
|---|---|---|---|
| `screen` | `width`, `height` | int | Logical panel size. Gearscope lowers the GUI scale while the screen is open if this size does not fit (client config `autoScale`). |
| `background` | `texture` | resource location | Background texture. |
| | `texture_width`, `texture_height` | int | Real texture size; it is drawn scaled to the panel size. |
| `armor` | `x` | int | X of the armor column. |
| | `y` | int[4] | Y of head, chest, legs, feet slots. |
| `offhand` | `x`, `y` | int | Offhand slot. |
| `inventory` | `x` | int | X of the first inventory column. |
| | `row_y` | int[3] | Y of the three main inventory rows. |
| | `slot_step` | int | Distance between slot columns (also used by the hotbar). |
| `hotbar` | `y` | int | Y of the hotbar. |
| `crafting` | `x`, `y` | int[2] | Columns and rows of the 2x2 crafting grid. |
| | `result_x`, `result_y` | int | Crafting result slot. |
| | `arrow_y` | int | Y of the arrow between grid and result. |
| `places` | `<name>` | int[2] | Named slot places `[x, y]` used by `slot_places.json`. |
| `tabs` | `x`, `y`, `step`, `width`, `height` | int | Accessory category tabs (vertical list). |
| | `visible` | int | Number of tabs shown at once. |
| | `scroll_up_y`, `arrow_width`, `arrow_height` | int | Tab scroll arrows. |
| `tab_slots` | `x` | int[] | Columns of the slots of the selected tab. |
| | `y`, `step`, `rows` | int | First row, row distance and rows per page. |
| | `page_prev_x`, `page_next_x` | int | Page arrows. |
| `cosmetic_button` | `x`, `y`, `size` | int | Cosmetic toggle button. |
| `recipe_button` | `x`, `y`, `size` | int | Polymorph's recipe selection button (only with Polymorph installed). |
| `player` | `center_x` | int | Horizontal center of the player figure. |
| | `feet_y` | int | Y where the feet of the figure stand. |
| | `scale` | int | Render scale of the figure. |
| | `box_width`, `box_height` | int | Size of the render box (mouse follow area), centered on the figure. |
| | `y_offset` | float | Vertical render offset passed to the vanilla entity renderer. |
| `attributes` | `x1`, `y1`, `x2`, `y2` | int | Attribute panel bounds. |
| | `list_y` | int | Y where the scrollable list starts. |
| | `checkbox_size`, `scrollbar_width`, `header_height`, `row_height`, `group_gap`, `icon_size` | int | Attribute list metrics. |
