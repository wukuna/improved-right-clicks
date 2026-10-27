# Improved Right-Clicks

Make right-click menus easier to read and harder to misclick. Choose independent protections for inventory items, banking and interface buttons; hide unwanted options; and make useful actions stand out with colours, ground-item icons and an extra-options indicator. Save profiles for different activities and use a reveal modifier when you need a hidden option. Protection switches start off.

Configure features in the normal plugin settings; use the side panel for profiles and the released import tools. Hold the configured reveal modifier before opening a menu to reveal options hidden by this plugin.

Previously **Menu Workshop**. Your saved settings and profiles keep their existing storage keys.

[Features](#features) · [Getting started](#getting-started) · [Usage](#usage) · [Saved settings](#saved-settings) · [Compatibility](#compatibility) · [Inspiration and recipe](#inspiration-and-recipe) · [Development](#development) · [Support](#support) · [License and attribution](#license-and-attribution)

## Features

- **Inventory:** protect potion Empty, hide selected item actions by name, and support item-on-item Use targets.
- **Bank:** independently hide inventory Wield, Wear, Drink and Eat while the bank is open. Deposit and withdraw actions are preserved.
- **Examine:** separate inventory, ground-item, NPC and scenery switches, with options to keep Examine when it is the only action.
- **Menu cleanup:** independently hide Cancel, Walk here and Set heading in opened right-click menus. Normal left-click walking/heading and button-protection fallback remain available. Cancel is retained when hiding it would leave an empty menu.
- **Target protection:** hide item Use on players, with a configurable allowed-item list; spell casts and other player actions are preserved. Hide Telekinetic Grab on NPCs while preserving ground-item casts, other spells, known useful targets and custom NPC-name exceptions.
- **Tools:** hide tree Chop/Cut or rock Mine when the required axe or pickaxe is neither carried nor equipped. Unknown inventory state leaves actions available.
- **House:** hide building actions outside building mode, plus unnecessary Mahogany Homes Remove actions. Other furniture actions stay available.
- **Buttons:** normal, right-click-only or hidden modes for Auto Retaliate, XP Drops, bank placeholders and Report. Right-click-only checks where the click began; it does not merely move the action down the list.
- **Presentation:** action colors, wildcard mappings, submenu support, a Shift-menu color picker/reset with configurable default color, optional ground-loot icons and additional-option counts. Counts support a minimum threshold, including/excluding Examine, showing/hiding counts on interface widgets, and a configurable text color.
- **Settings:** configure every feature in RuneLite's normal Improved Right-Clicks settings, grouped into collapsible sections. **Hide options** groups Walk here, Cancel, Set heading, Use on Player, Telegrab NPCs, potion Empty and Examine controls. Bank stays last.
- **Side panel tools:** a direct Open Plugin Settings shortcut, named profiles, twenty-step undo, validated import/export previews, and collapsible diagnostics showing the before/after entry counts and reasons. Closing a dialog leaves the last operation status unchanged; applying identical settings reports No changes. Settings are not duplicated in the side panel. Undo restores only settings changed by a profile/import/editor operation, leaving later manual changes alone. Operations that change nothing do not consume an undo step; ordinary RuneLite settings use RuneLite's normal controls.

Protection switches start off. Enable the features you want in RuneLite's Improved Right-Clicks settings. Button modes start at Normal. Hold **Ctrl before opening a menu** to reveal options hidden by this plugin. The reveal modifier can be changed to Shift, Alt or None. It does not consume keys or recreate options removed by another plugin.

Enable **Hide side panel** under General to remove the Improved Right-Clicks toolbar button. Menu features and the Shift-menu color editor remain active. Turn it off to access profiles, import/export, undo and diagnostics again.

## Getting started

1. Open **Improved Right-Clicks** in RuneLite's plugin settings and enable the protections or presentation options you want.
2. Use the side panel for profiles, imports, custom icon setup and diagnostics.
3. Hold the configured reveal modifier before opening a menu to temporarily show options this plugin hid.

**Release status:** The Plugin Hub currently distributes the manual filter-import workflow described below. Automatic live icon sharing with Loot Filters remains in development and is not part of that release.

## Usage

### Item rules and exceptions

Use one `action,item name` rule per line. Matching ignores case and formatting tags. `*` matches any text; other characters are literal. Lines starting with `#` are comments.

```text
Drop,Rune*
[bank] Eat,Shark
[ground] Take,Coins
Use,* -> Torn clue scroll*
```

Optional contexts are `[inventory]`, `[bank]`, `[ground]` and `[all]`. Untagged rules apply to supported item entries in all contexts. Rules never filter player actions, NPC combat actions or item/spell use on actors.

Exceptions use one name per line, with the same optional contexts and wildcard matching. For example, `[ground] Coins` keeps ground-item Coins options while leaving inventory rules active. These exceptions override item, Examine, tool and house filtering; button protection and target protection have their own settings.

**No Empty on potions** hides Empty on dosed inventory potions while retaining Drink. It starts off, like the other protection switches.

**No Use on Player** allows common quest, Barbarian Assault and cooperation items by default. Edit **Allow items on players** with exact item names separated by commas. **Don't Telegrab NPCs** always permits the Maze Guardian, King's Ransom Guard, Wyrmscraig hunter goats, Sailing creature corpses and lost Sailing crates. Add other exact names under **Allow Telegrab NPC names**. These controls do not support arbitrary actor/combat filtering. Missing selection or target information leaves the option available. The shared reveal modifier bypasses both protections.

Menu colors use `target:action:#RRGGBB`, separated by semicolons:

```text
*:Take:#88cc88;Coins:Take:#ffcc33
```

Exact color mappings take precedence over wildcard mappings. Hold Shift when opening a menu to use the local color editor. Reset removes that entry's exact mapping; a broader wildcard can still apply.

**Yellow hover highlight** is enabled by default: a colored main-menu option uses the native yellow highlight while hovered, then returns to its configured color. Submenu entries retain their configured colors because RuneLite does not expose which submenu is open for reliable hover detection. Disable it to retain the configured color on hover. Item/name colors and image tags are preserved.

**Default picker color** applies when an entry has no matching color mapping. **Option count text color** changes the hover count. Both colors use `#RRGGBB` in RuneLite settings. The Shift menu color editor provides an interactive picker for menu entries. **Minimum extra options** accepts 1–500. Counts exclude Cancel, Walk here and local editor actions; **Count Examine** and **Show count on interface widgets** control the remaining categories. These settings are included in profiles, undo and imports.

The editor accepts at most 200 rules per field. Invalid manual configuration edits disable that rule field rather than retaining old invisible rules. Profile imports validate every setting before applying changes. Imports cannot modify other plugins' settings. Existing-setting import reads explicitly saved settings from the supported plugins listed below and leaves those originals intact. Missing settings and source-plugin enablement are not inferred.

### Custom ground-menu icons

Enable ground-loot menu icons and select **Native**, **Custom**, or **Both** in the normal plugin settings. Open **Ground loot menu icons** in the side panel to import a filter or manage custom icons. Native shows item pictures; Custom shows imported or manually assigned icons; Both shows custom and item pictures together. Custom leaves items without a visible assigned icon undecorated; it never substitutes a native icon for a missing custom image. Both retains the native item picture when no custom icon is visible. Imported filters honor the final `hidden` and `hideOverlay` values after matching rule overrides. Manually assigned item icons take priority over imported filter rules. Both avoids duplicating Current Item or the same item ID. Menu colors stay separate.

Enable **Both: respect filter visibility** under Presentation to show both icons only when a visible custom icon exists. Hidden, unassigned or unavailable custom icons then suppress both pictures. The option defaults off, does not affect Native mode, and is included in profiles, import/export and undo. Manual assignments remain explicit overrides.

**Loot Filters is not linked automatically.** Click **Import Loot Filters script** and import the same Filterscape export you use there. Confirm the import; its rule count appears in the side panel. Import again when you change that script. The original Loot Filters setup is never modified.

Choose **Manage icons / backups** to assign Heart, Star, Diamond, Current Item or an image file to an exact item name. Use tradeable-item search or enter an untradeable item's exact name. Manual assignments take priority over the imported filter; the editor labels them separately from imported rules.

**Import Loot Filters** accepts a generated Filterscape text file (`.rs2f` or `.txt`) or pasted text. It preserves the original RS2F source and evaluates macros (including parameters), boolean conditions, wildcard names, item IDs, quantity and value comparisons, tradeability, noted/stackable state, ownership, account type, and areas. `apply` inherits icon values; terminal `rule`/`if` stops at the first matching rule, including rules without an explicit icon. These rules choose icons beside **Take** menu entries only. Ground overlays, hiding, sorting, sounds, loot beams and colors are not activated by this import.

`CurrentItem()`, `Item(id)`, `Sprite(id,index)` and `File("name.png")` icons are supported. If a filter references files, select its icons directory (usually `.runelite/loot-filters/icons`). Every referenced image must be available before the import applies. PNG, JPEG and GIF are decoded to a transparent, aspect-preserving 17 x 15 menu image; GIF uses its first frame. Copies are embedded in our configuration, so uninstalling Loot Filters or removing the original files does not break the imported setup. Invalid paths, missing files and malformed filters leave the previous setup intact. Limits: 1 MB filter text, 128 images, 2 MB / 2048 x 2048 per source image, bounded macro expansion and rule nesting.

Review the rule/image counts before applying. A new import replaces the previous imported filter; manual assignments stay. **Undo filter import** swaps with the previous saved filter. **Filter setup / backup** exports/imports a self-contained JSON bundle with compressed original source and embedded images, or disables the imported filter. **Import/export icons** and **Undo / redo** separately manage manual assignments. Data is saved per RuneLite profile in our `improvedmenus` configuration. Imports never modify Loot Filters files or settings and never fetch a filter or image from the network.

Menu entries do not always identify which of several identical ground stacks they refer to. If those stacks would produce different visible icons, we omit the custom icon rather than guess. Both keeps the native picture unless **Both: respect filter visibility** is enabled. Submenu icon drawing remains unsupported by the existing public menu-bounds integration.

The RS2F lexer, preprocessor, parser, condition classes and preamble are adapted from [riktenx/loot-filters at 993fd054](https://github.com/riktenx/loot-filters/tree/993fd054e08d42a77d3415acbb9ac7c2797726ed), under its [BSD 2-Clause license](src/main/resources/com/improvedmenus/loot-filters-LICENSE.txt). Our adapter supplies read-only ground-item facts and consumes only the chosen icon; visibility properties select whether an icon is shown; other display/audio properties never change ground rendering, menu entries or colors. The imported setup works independently; automatic integration with the upstream plugin is being discussed separately and is not required for imports. The supplied Filterscape, Joe and Ironman presets are used as local compatibility fixtures, not bundled in the plugin.

### Additional option indicator

Under Presentation, **Hide player option counts** excludes player actions while retaining loot and object actions underneath them. **Hide counts when Walk here** suppresses deprioritized NPC/object counts when walking is preferred; ground items and widgets remain eligible. These switches affect only the hover indicator, not the menu entries. Both start off.

**Option count anchor** offers Below left (the existing default), Below right, Above left and Above right relative to the tooltip stack. RuneLite still controls the stack position relative to the cursor. All options are saved in profiles and support import/export and undo.

## Saved settings

Settings and named profiles retain the existing `improvedmenus` storage keys. Renaming the plugin or repository does not reset them. Profiles, validated imports and the side panel's Undo tools manage plugin settings; custom icon assignments and imported filter backups have their own controls described above. Imports leave other plugins' original settings and files intact.

## Compatibility

Users of the features in this table can consolidate them here after checking their settings. This is feature coverage, not a claim that every setting in each source plugin has been reproduced. Cancel/Walk/Set-heading cleanup applies only to opened menus. The extra-options indicator has its own placement controls. Useful Telegrab targets are always allowed rather than individually switchable. Hold-to-reveal is shared rather than Examine-only. Mahogany Homes cleanup preserves non-Remove actions.

Keep RuneLite's Menu Entry Swapper for swaps and specialist plugins for their remaining features. Disable overlapping filters/colors before comparing results: this plugin cannot reveal options another plugin has removed.

Menu Entry Swapper's Shift-right-click customization remains available with Examine hiding and menu colors enabled. Improved Right-Clicks keeps its native Examine lookup entries unchanged while building that menu, then applies the configured cleanup after the swap controls have been added.

Automatic Loot Filters integration is not part of the current public import workflow. Import the same Filterscape script manually as described under Usage. Imported icons decorate menu entries; they do not change ground labels, beams or sounds.

## Inspiration and recipe

The recipe combines focused menu protections, readable actions and useful visual cues. The plugins below supplied either adapted components and item lists or the feature ideas behind our shared menu controls. Original authors and the exact adapted parts are documented in the code notices; related swap and loot tools remain useful for their own jobs.

| Project | Connection | What it contributes or complements |
| --- | --- | --- |
| [No Examine](https://github.com/Skretzo/runelite-plugins/tree/ea506dbbfd2aa70dd024f2fc7b761786e2ced91a) | Feature inspiration | Category-specific Examine removal, lone-Examine exceptions and opened-menu cleanup. |
| [Not Empty](https://github.com/dekvall/runelite-external-plugins/tree/0c1c5cf2a8588ac3b017e7026c393d2eee4980c3) | Feature inspiration | Protection against emptying dosed items by mistake. |
| [No Misclicks In Bank](https://github.com/Welkon1/No-Misclicks-In-Bank) | Feature inspiration | Separate Wear, Wield, Drink and Eat guards while banking. |
| [Item Option Hider](https://github.com/kekingbd/item-option-hider) | Feature inspiration | Item/action matching, wildcards and item-on-item targets. |
| [Menu Colorizer](https://github.com/dgalambos/menu-colorizer) | Feature inspiration | Action colours and an interactive menu colour picker. |
| [Tool Required](https://github.com/Unmoon/tool-required) | Adapted ingredient | Reviewed tool lists and special targets for axe/pickaxe checks; credit to Unmoon. |
| [Home Improvement](https://github.com/Mike-U5/rl-plugin-home-improvement) | Adapted ingredient | Furniture mappings for house and Mahogany Homes cleanup; credit to Mike-U5. |
| [Misclick Guard](https://github.com/willzero123/misclick-guard) | Adapted ingredient | Protected-button handling, click-origin checks and Cancel fallback; credit to willzero123. |
| [Ground Loot Icons](https://github.com/Fiffers/ground-loot-icons) | Adapted ingredient | Menu-icon placement and row geometry; original Louis Hong copyright is retained. |
| [Mouseover Options+](https://github.com/Petzep/mouseover-options) | Feature inspiration | Extra-option counts, thresholds and display choices. |
| [No Use Player](https://github.com/oohwooh/no-use-players) | Adapted ingredient | The allowed-item list for player-use protection; credit to Lola Egherman. |
| [Don't Telegrab NPCs](https://github.com/ldavid432/dont-telekinetic-grab-npcs) | Adapted ingredient | Useful-target exceptions for Telegrab; credit to ldavid432. |
| [Menu Entry Swapper (built in)](https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins/menuentryswapper) | Optional companion | Changes preferred actions. Improved Right-Clicks concentrates on visibility, protection and presentation; existing swap customization remains available. |
| [Custom Menu Swaps](https://github.com/geheur/More-menu-entry-swaps) | Specialist companion or alternative | Text-based swaps, hiding and hotkeys. Avoid duplicate hide rules; our reveal modifier cannot recover options another plugin removes. |
| [Menu Swapper Extended](https://github.com/raiyni/menu-swapper-extended) | Specialist companion | Additional targeted swaps. Review its rules alongside any overlapping menu protections. |
| [Ground Items (built in)](https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins/grounditems) | Optional ground-label tool | Ground labels, values and highlighting; menu icons and action colours are a separate presentation layer. |
| [Loot Filters / Filterscape](https://github.com/riktenx/loot-filters) | Adapted RS2F ingredients; separate loot tool | The released manual importer uses adapted parser components and supports Filterscape scripts. Automatic live icon sharing remains in development; it is not supplied by ordinary Loot Filters today. |

The [Filterscape editor](https://filterscape.xyz/) prepares Loot Filters scripts. Importing one into Improved Right-Clicks affects menu icons, not ground labels, beams or sounds. Feature overlap does not imply complete replacement. Full licenses and adaptation details remain in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Development

Java 11 is used for the build. Run `gradlew.bat test` for automated checks or `gradlew.bat run` to launch the development client. Tests cover filtering boundaries, order/callback preservation, contexts, profiles/import validation, click origin, tooltip ownership, reveal and cosmetic restoration. See the build configuration and third-party notices for dependencies and licensing.

A short live smoke check is still needed for real menu rendering: try a dosed potion and bank item, one protected button through both click paths, a tree with/without an axe, a house menu outside/inside building mode, a colored submenu, and ground loot icons. Disable/re-enable once while a menu is open. Extensive repeated testing is not required, but automated checks do not verify actual in-game rendering.

Ground loot icons decorate the main right-click menu only. Submenus keep their original target text because RuneLite does not expose whether a submenu is open for reliable icon positioning. Reopening a menu and disabling icons restore the target text owned by Improved Right-Clicks.

Tool guards leave the knife-cut Temple Trekking swamp vines and quest Cave entrance interactions available. Ordinary axe and pickaxe protections remain independent.

## Support

[Report an issue or suggest a feature](https://github.com/wukuna/improved-right-clicks/issues). Include the affected setting, steps to reproduce, other menu plugins and a screenshot when useful. For imported icons, include a small example filter with private details removed.

## License and attribution

Improved Right-Clicks uses the [BSD 2-Clause license](LICENSE). Adapted projects, bundled RS2F components and their licenses are documented in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and the resource notices. Source attribution is separate from feature compatibility.
