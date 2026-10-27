# Other mods and source acknowledgments

These projects informed the feature design. Adapted code and catalogs are noted below; their licenses are distributed in `src/main/resources/META-INF/licenses`.

Adaptations in this release:

- `TargetProtections` retains No Use Player's default allowed-item list (Copyright 2024 Lola Egherman) and Don't Telegrab NPCs' useful-target catalog (Copyright 2025 ldavid432). Both complete BSD 2-Clause licenses are retained in its header and the distributed license resources. The selection checks and menu filtering are newly implemented for the shared engine.

- `ButtonGuard`, `GuardedAction` and `ClickMode` derive from Misclick Guard (Copyright 2026 willzero123). Original BSD 2-Clause headers are retained; client-thread cleanup, shared reveal and captured-widget restoration were added.
- `ToolCatalog` derives from Tool Required (Copyright 2024 Unmoon). It retains the published axe/pickaxe list and special tree targets without depending on the clue-scroll plugin implementation.
- `HomeCatalog` derives from Home Improvement (Copyright 2024 Mike-U5), including its Mahogany Homes furniture mappings. House region constants were checked against No Examine. The new implementation preserves non-Remove furniture actions.
- `MenuPresentation` adapts Ground Loot Icons' row geometry and placeholder technique (repository Fiffers/ground-loot-icons; its license retains Copyright 2024 Louis Hong). Registration is reused on re-enable, with owned marker cleanup.
- The other filtering, coloring, profiles, editor and count implementation is independently written from the documented behavior. The README credits all feature sources and states coverage differences.

Each complete source license is retained in the corresponding file below `src/main/resources/META-INF/licenses`, including copyright notices, redistribution conditions and disclaimers. The jar includes these same resources for binary distribution.

## no-use-player

```
repository=https://github.com/oohwooh/no-use-players.git
commit=e76b5612cea2f9fe68b0ba6cecd2bf11b1a5d656
```

## dont-telegrab-npcs

```
repository=https://github.com/ldavid432/dont-telekinetic-grab-npcs.git
commit=71b6675364d67a65eb0023667c42863eeb0e3ea7
```

## ground-loot-icons

```
repository=https://github.com/Fiffers/ground-loot-icons.git
commit=8e3e529f27aef3b7a5323ef8fef4f565d8b00be2
authors=Fiffers
```

## home-improvement

```
repository=https://github.com/Mike-U5/rl-plugin-home-improvement.git
commit=6b7cca56d95d794bae252f5f7b58b8d2d5f6027e
```

## item-option-hider

```
repository=https://github.com/kekingbd/item-option-hider.git
commit=5375d699a8661c157c7e26b699a9c46401237c7e
```

## menu-colorizer

```
repository=https://github.com/dgalambos/menu-colorizer.git
commit=3e73b989615aecb2813f0b74bb9ecdf5ba5451ac
```

## misclick-guard

```
repository=https://github.com/willzero123/misclick-guard.git
commit=a49e33e7576dbf60ec3caa39be4f470b6be71f47
```

## mouseover-options

```
repository=https://github.com/Petzep/mouseover-options.git
commit=872f6bf6a0817e5d6292749c2f3d4ae8a8eb9600
```

## no-examine

```
repository=https://github.com/Skretzo/runelite-plugins.git
commit=ea506dbbfd2aa70dd024f2fc7b761786e2ced91a
```

## no-misclicks-in-bank

```
repository=https://github.com/Welkon1/No-Misclicks-In-Bank.git
commit=6f76aa102a04525bdce2424b57b4ad899ba82e8b
```

## not-empty

```
repository=https://github.com/dekvall/runelite-external-plugins.git
commit=0c1c5cf2a8588ac3b017e7026c393d2eee4980c3
```

## tool-required

```
repository=https://github.com/Unmoon/tool-required.git
commit=031e4986c2235f58888cf158666a02f3497cac60
```

## Loot Filters RS2F components

The released manual importer contains adapted RS2F lexer, parser, preprocessor, condition and utility components from [riktenx/loot-filters](https://github.com/riktenx/loot-filters), copyright (c) 2025 riktenx, BSD 2-Clause. Adapted files identify this origin. The complete license is distributed in `src/main/resources/com/improvedmenus/loot-filters-LICENSE.txt` and `META-INF/licenses/loot-filters.txt`. Our menu-icon evaluation and import interface do not establish automatic integration with ordinary Loot Filters. This attribution remains required independently of later integration plans.
