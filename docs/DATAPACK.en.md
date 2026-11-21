# Creating your own theme

[🇫🇷 Français](DATAPACK.fr.md) | 🇬🇧 English

A theme decides what the league screen shows: colors, where the Pokémon go, trainer portraits, images and texts. Themes are loaded with a **data pack**, and the mod already ships one: **Crown** (`couronne`).

A theme added by a data pack shows up in the **Theme** tab of the registration terminal: sneak and right-click the terminal.

## Data pack structure

```
my_pack/
├── pack.mcmeta
└── data/
    └── my_pack/
        └── leaguescreen/
            └── themes/
                └── my_theme.json
```

`pack.mcmeta` (Minecraft 1.21.1):

```json
{
  "pack": {
    "pack_format": 48,
    "description": "My league screen themes"
  }
}
```

Put the folder or `.zip` in `saves/<your world>/datapacks/` (or `world/datapacks/` on a server), then run `/reload`.

The theme's id comes from the file's location: `data/my_pack/leaguescreen/themes/kanto/indigo.json` gives `my_pack:kanto/indigo`.

> **Replacing Crown:** a file at `data/cobblemon-league-screen/leaguescreen/themes/couronne.json` replaces the default theme. If it has an error, the mod keeps its own Crown.

## Minimal example

```json
{
  "name": "My theme",
  "background": "#101820",
  "colors": { "left": "#2E86DE", "right": "#EE5253", "accent": "#FECA57" },
  "live": [
    { "type": "name", "side": "left",  "x": 100, "y": 40, "w": 500, "h": 60 },
    { "type": "name", "side": "right", "x": 900, "y": 40, "w": 500, "h": 60 },
    { "type": "slot", "side": "left",  "index": 1, "x": 100, "y": 700, "w": 120, "h": 120 },
    { "type": "slot", "side": "right", "index": 1, "x": 1280, "y": 700, "w": 120, "h": 120 }
  ],
  "victory": [
    { "type": "portrait", "side": "winner", "style": "figure", "x": 600, "y": 100, "w": 300, "h": 520 },
    { "type": "text", "value": "VICTORY", "color": "@accent", "x": 150, "y": 700, "w": 1200, "h": 110 }
  ]
}
```

For a complete example, look at the mod's [couronne.json](../src/main/resources/data/cobblemon-league-screen/leaguescreen/themes/couronne.json) theme.

## The canvas

A theme is drawn on a **canvas** measured in pixels, like a mockup in Canva: the origin `(0, 0)` is at the **top left** and `y` goes down. On the wall, the canvas is scaled to fit the screen while keeping its proportions, then centered. The rest of the wall uses the `background` color.

## Theme fields

| Field | Default | Description |
|---|---|---|
| `name` | the id | Name shown in the menu. Accepts plain text or a translation key. |
| `canvas` | `[1500, 900]` | Canvas size: `[width, height]`. |
| `background` | `#000000` | Wall background color. |
| `victoryBackground` | `background` | Background color during the victory screen. `@side` means the winner here. |
| `colors` | Crown's | Theme palette: `left`, `right`, `accent`, or any name of your own. |
| `pokemon` | Crown's | How Pokémon are shown in their slots (see below). |
| `slots` | Crown's | Slot images for each Pokémon state (see below). |
| `live` | empty | Layers of the **live** scene, during the battle. |
| `victory` | empty | Layers of the **victory** scene. |

If you leave out `colors`, `pokemon` or `slots`, the theme uses Crown's. You only need to write what you change.

## Colors

A color can be written three ways:

- `#RRGGBB`: for example `#FF7A1F`.
- `#AARRGGBB`: with opacity, for example `#00000000` for transparent.
- `@name`: a palette color, for example `@accent`. `@side` uses the color of the layer's side (`left` or `right`).

Add `:factor` to darken it: `@side:0.45` keeps 45% of the brightness.

## Layers

Layers are drawn in list order: **the first one is at the back**. Each layer has a box `x`, `y`, `w`, `h` (`w` and `h` are required), plus an optional `alpha` from 0 to 1.

| `type` | Fields | Description |
|---|---|---|
| `fill` | `color` | A colored rectangle. |
| `image` | `image`, `color` | An image, tinted by `color` (white by default, so no tint). |
| `text` | `value`, `color`, `align` | A text, or a translation key. |
| `name` | `side`, `color`, `align` | The trainer's name. |
| `portrait` | `side`, `style`, `frame`, `fill`, `border`, `yaw`, `pitch` | The trainer (see below). |
| `slot` | `side`, `index`, `style` | A team slot (see below). |

- `side`: `left`, `right`, or `winner` (in the `victory` scene only).
- `align`: `left`, `center` (default) or `right`.
- `image`: a texture id, for example `cobblemon-league-screen:textures/board/halo.png`.

A layer with an unknown type is skipped, and a warning is written to the logs.

### Portrait

| Field | Default | Description |
|---|---|---|
| `style` | `bust` | `bust`: the skin's bust, flat. `figure`: the whole trainer, in 3D. |
| `frame` | `@side` | Frame color. |
| `fill` | `@side:0.25` | Frame background color. |
| `border` | 6% | Frame thickness, in canvas pixels. |
| `yaw` | `0` | Rotation in degrees (`figure`), toward the center of the screen. The right side is mirrored. |
| `pitch` | `0` | Head tilt, from -60 to 60 (positive: looks down). |

For a trainer with no frame, set `"frame": "#00000000"`, `"fill": "#00000000"` and `"border": 0`.

### Slots (`slot`)

`index` goes from 1 to 6: it's the Pokémon's place in the team. A slot shows the images for its state, then the Pokémon on top.

The images for each state are set in `slots`, and a slot can have its own `style` to override them:

| State | When |
|---|---|
| `empty` | No Pokémon in this place (team of fewer than six). |
| `hidden` | Pokémon not sent out yet. |
| `bench` | Already seen, currently in its Poké Ball. |
| `active` | On the field. |
| `fainted` | Fainted. |

Each state is a list of images, centered on the slot:

```json
"active": [
  { "image": "cobblemon-league-screen:textures/board/halo.png", "color": "@accent", "scale": 1.6 },
  { "image": "cobblemon-league-screen:textures/board/circle.png", "color": "#000000", "alpha": 0.35 }
]
```

`color` (white by default), `alpha` (1 by default) and `scale` (1 by default) are optional.

### Pokémon

```json
"pokemon": { "fit": 0.85, "yaw": 35, "depth": 0.15, "fainted": "#4A4A4A" }
```

| Field | Description |
|---|---|
| `fit` | Pokémon size in its slot (1 = the whole slot). |
| `yaw` | Rotation in degrees, toward the center of the screen. |
| `depth` | Thickness of the 3D model (smaller = flatter). |
| `fainted` | Tint of a fainted Pokémon. |

## Custom images

A data pack only holds the theme's JSON. **Images** are loaded on each player's side, so your own images must go in a **resource pack**:

```
my_resource_pack/
├── pack.mcmeta            ← "pack_format": 34
└── assets/
    └── my_pack/
        └── textures/
            └── board/
                └── background.png
```

In the theme: `"image": "my_pack:textures/board/background.png"`. On a server, share it as the server resource pack.

If a player doesn't have the image, the layer is simply not drawn for them.

**Images shipped with the mod**, usable without a resource pack:

- `cobblemon-league-screen:textures/board/circle.png`
- `cobblemon-league-screen:textures/board/ring.png`
- `cobblemon-league-screen:textures/board/halo.png`
- `cobblemon-league-screen:textures/board/paint_border.png`
- `cobblemon-league-screen:textures/board/paint_border2.png`
- `cobblemon-league-screen:textures/board/paint_vs.png`

Cobblemon's textures work too, for example `cobblemon:textures/item/poke_balls/poke_ball.png`.

## Troubleshooting

If your theme doesn't show up in the menu, check the server logs after `/reload`. A theme with an error is rejected entirely, and the message names the field at fault (the logs are in French):

```
Thème écarté — my_pack:my_theme : live[3].w : champ obligatoire
```
