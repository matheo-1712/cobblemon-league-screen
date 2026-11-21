# Créer son propre thème

🇫🇷 Français | [🇬🇧 English](DATAPACK.en.md)

Un thème décide de ce que l'écran de ligue affiche : les couleurs, la place des Pokémon, les portraits des dresseurs, les images et les textes. Les thèmes se chargent avec un **data pack**, et le mod en contient déjà un, la **Couronne**.

Un thème ajouté par data pack apparaît dans l'onglet **Thème** du terminal d'inscription : accroupis-toi et fais un clic droit sur le terminal.

## Structure du data pack

```
mon_pack/
├── pack.mcmeta
└── data/
    └── mon_pack/
        └── leaguescreen/
            └── themes/
                └── mon_theme.json
```

`pack.mcmeta` (Minecraft 1.21.1) :

```json
{
  "pack": {
    "pack_format": 48,
    "description": "Mes thèmes d'écran de ligue"
  }
}
```

Place le dossier ou le `.zip` dans `saves/<ton monde>/datapacks/` (ou dans `world/datapacks/` sur un serveur), puis lance `/reload`.

L'identifiant du thème vient de l'emplacement du fichier : `data/mon_pack/leaguescreen/themes/kanto/indigo.json` donne `mon_pack:kanto/indigo`.

> **Remplacer la Couronne :** un fichier `data/cobblemon-league-screen/leaguescreen/themes/couronne.json` remplace le thème par défaut. S'il contient une erreur, le mod garde sa propre Couronne.

## Exemple minimal

```json
{
  "name": "Mon thème",
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
    { "type": "text", "value": "VICTOIRE", "color": "@accent", "x": 150, "y": 700, "w": 1200, "h": 110 }
  ]
}
```

Pour un exemple complet, regarde le thème [couronne.json](../src/main/resources/data/cobblemon-league-screen/leaguescreen/themes/couronne.json) du mod.

## Le canevas

Un thème se dessine sur un **canevas** en pixels, comme une maquette dans Canva : l'origine `(0, 0)` est en **haut à gauche** et `y` descend. Sur le mur, le canevas est agrandi pour tenir dans l'écran en gardant ses proportions, puis centré. Le reste du mur prend la couleur `background`.

## Champs du thème

| Champ | Par défaut | Description |
|---|---|---|
| `name` | l'identifiant | Nom affiché dans le menu. Accepte un texte ou une clé de traduction. |
| `canvas` | `[1500, 900]` | Taille du canevas : `[largeur, hauteur]`. |
| `background` | `#000000` | Couleur de fond du mur. |
| `victoryBackground` | `background` | Couleur de fond pendant l'écran de victoire. `@side` y désigne le vainqueur. |
| `colors` | celles de la Couronne | Palette du thème : `left`, `right`, `accent`, ou n'importe quel nom à toi. |
| `pokemon` | celui de la Couronne | Affichage des Pokémon dans leurs cases (voir plus bas). |
| `slots` | ceux de la Couronne | Images des cases selon l'état du Pokémon (voir plus bas). |
| `live` | vide | Calques de la scène **en direct**, pendant le combat. |
| `victory` | vide | Calques de la scène de **victoire**. |

Si tu n'écris pas `colors`, `pokemon` ou `slots`, le thème reprend ceux de la Couronne. Tu n'as donc à écrire que ce que tu changes.

## Couleurs

Une couleur s'écrit de trois façons :

- `#RRGGBB` : par exemple `#FF7A1F`.
- `#AARRGGBB` : avec opacité, par exemple `#00000000` pour du transparent.
- `@nom` : une couleur de la palette, par exemple `@accent`. `@side` prend la couleur du camp du calque (`left` ou `right`).

Ajoute `:facteur` pour assombrir : `@side:0.45` garde 45 % de la luminosité.

## Calques

Les calques sont dessinés dans l'ordre de la liste : **le premier est derrière**. Chaque calque a une boîte `x`, `y`, `w`, `h` (`w` et `h` sont obligatoires), plus une option `alpha` qui va de 0 à 1.

| `type` | Champs | Description |
|---|---|---|
| `fill` | `color` | Un rectangle de couleur. |
| `image` | `image`, `color` | Une image, teintée par `color` (blanc par défaut, donc sans teinte). |
| `text` | `value`, `color`, `align` | Un texte, ou une clé de traduction. |
| `name` | `side`, `color`, `align` | Le nom du dresseur. |
| `portrait` | `side`, `style`, `frame`, `fill`, `border`, `yaw`, `pitch` | Le dresseur (voir plus bas). |
| `slot` | `side`, `index`, `style` | Une case de l'équipe (voir plus bas). |

- `side` : `left`, `right`, ou `winner` (dans la scène `victory` seulement).
- `align` : `left`, `center` (par défaut) ou `right`.
- `image` : un identifiant de texture, par exemple `cobblemon-league-screen:textures/board/halo.png`.

Un calque d'un type inconnu est ignoré, et un avertissement est écrit dans les logs.

### Portrait

| Champ | Par défaut | Description |
|---|---|---|
| `style` | `bust` | `bust` : le buste du skin à plat. `figure` : le dresseur entier, en 3D. |
| `frame` | `@side` | Couleur du cadre. |
| `fill` | `@side:0.25` | Couleur du fond du cadre. |
| `border` | 6 % | Épaisseur du cadre, en pixels du canevas. |
| `yaw` | `0` | Rotation en degrés (`figure`), vers le centre de l'écran. Le camp de droite est mis en miroir. |
| `pitch` | `0` | Inclinaison de la tête, de -60 à 60 (positif : il baisse les yeux). |

Pour un dresseur sans cadre, mets `"frame": "#00000000"`, `"fill": "#00000000"` et `"border": 0`.

### Cases (`slot`)

`index` va de 1 à 6 : c'est la place du Pokémon dans l'équipe. Une case affiche les images de son état, puis le Pokémon par-dessus.

Les images de chaque état se règlent dans `slots`, et une case peut avoir son propre `style` pour les remplacer :

| État | Quand |
|---|---|
| `empty` | Pas de Pokémon à cette place (équipe de moins de six). |
| `hidden` | Pokémon pas encore envoyé. |
| `bench` | Déjà vu, actuellement dans sa Poké Ball. |
| `active` | Sur le terrain. |
| `fainted` | K.O. |

Chaque état est une liste d'images, centrées sur la case :

```json
"active": [
  { "image": "cobblemon-league-screen:textures/board/halo.png", "color": "@accent", "scale": 1.6 },
  { "image": "cobblemon-league-screen:textures/board/circle.png", "color": "#000000", "alpha": 0.35 }
]
```

`color` (blanc par défaut), `alpha` (1 par défaut) et `scale` (1 par défaut) sont optionnels.

### Pokémon

```json
"pokemon": { "fit": 0.85, "yaw": 35, "depth": 0.15, "fainted": "#4A4A4A" }
```

| Champ | Description |
|---|---|
| `fit` | Taille du Pokémon dans sa case (1 = toute la case). |
| `yaw` | Rotation en degrés, vers le centre de l'écran. |
| `depth` | Épaisseur du modèle 3D (petit = plus plat). |
| `fainted` | Teinte d'un Pokémon K.O. |

## Images personnalisées

Un data pack ne contient que le JSON du thème. Les **images** sont lues chez chaque joueur, donc tes propres images doivent être dans un **resource pack** :

```
mon_resource_pack/
├── pack.mcmeta            ← "pack_format": 34
└── assets/
    └── mon_pack/
        └── textures/
            └── board/
                └── fond.png
```

Dans le thème : `"image": "mon_pack:textures/board/fond.png"`. Sur un serveur, distribue-le comme resource pack du serveur.

Si un joueur n'a pas l'image, le calque n'est simplement pas dessiné chez lui.

**Images fournies par le mod**, utilisables sans resource pack :

- `cobblemon-league-screen:textures/board/circle.png`
- `cobblemon-league-screen:textures/board/ring.png`
- `cobblemon-league-screen:textures/board/halo.png`
- `cobblemon-league-screen:textures/board/paint_border.png`
- `cobblemon-league-screen:textures/board/paint_border2.png`
- `cobblemon-league-screen:textures/board/paint_vs.png`

Les textures de Cobblemon marchent aussi, par exemple `cobblemon:textures/item/poke_balls/poke_ball.png`.

## En cas de problème

Si ton thème n'apparaît pas dans le menu, regarde les logs du serveur après `/reload`. Un thème avec une erreur est écarté en entier, et le message nomme le champ en cause :

```
Thème écarté — mon_pack:mon_theme : live[3].w : champ obligatoire
```
