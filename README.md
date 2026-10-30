# Improved Right-Clicks

Make right-click menus easier to read and harder to misclick. Choose independent protections for inventory items, banking and interface buttons; hide unwanted options; and make useful actions stand out with colours, ground-item icons and an extra-options indicator. Add individually selectable friend, ignored-player, Friends Chat and RuneLite party badges, or choose the optional OSRS menu theme. Save profiles for different activities and use a reveal modifier when you need a hidden option. Protection switches start off.

Configure features in the normal plugin settings; use the side panel for profiles, settings import/export and diagnostics. Hold the configured reveal modifier before opening a menu to reveal options hidden by this plugin.

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
- **Player badges:** independent green friend, ignored-player, Friends Chat and RuneLite party icons, plus optional Copy name. Existing rank icons are preserved; matching icons are not added twice.
- **Menu theme:** keep Native styling or choose OSRS brown and bronze. Menus with submenus or another plugin's custom renderer retain their original appearance.
- **Settings:** configure every feature in RuneLite's normal Improved Right-Clicks settings, grouped into collapsible sections. **Hide options** groups Walk here, Cancel, Set heading, Use on Player, Telegrab NPCs, potion Empty and Examine controls. Bank stays last.
- **Side panel tools:** a direct Open Plugin Settings shortcut, named profiles, twenty-step undo, validated import/export previews, and collapsible diagnostics showing the before/after entry counts and reasons. Closing a dialog leaves the last operation status unchanged; applying identical settings reports No changes. Settings are not duplicated in the side panel. Undo restores only settings changed by a profile/import/editor operation, leaving later manual changes alone. Operations that change nothing do not consume an undo step; ordinary RuneLite settings use RuneLite's normal controls.

Protection switches start off. Enable the features you want in RuneLite's Improved Right-Clicks settings. Button modes start at Normal. Hold **Ctrl before opening a menu** to reveal options hidden by this plugin. The reveal modifier can be changed to Shift, Alt or None. It does not consume keys or recreate options removed by another plugin.

Enable **Hide side panel** under General to remove the Improved Right-Clicks toolbar button. Menu features and the Shift-menu color editor remain active. Turn it off to access profiles, import/export, undo and diagnostics again.

**Availability:** Install **Improved Right-Clicks** from RuneLite's Plugin Hub. [Update #19763](https://github.com/runelite/plugin-hub/pull/19763) merged on 30 October 2026, including player badges, the OSRS theme and Improved Looting integration.

**More plugins:** [Banking, Looting, Right-Clicks, Interface Layout and Chat](https://github.com/wukuna#runelite-plugins).

## Getting started

1. Open **Improved Right-Clicks** in RuneLite's plugin settings and enable the protections or presentation options you want.
2. Use the side panel for profiles, settings import/export, undo and diagnostics.
3. Hold the configured reveal modifier before opening a menu to temporarily show options this plugin hid.

**Compatibility status:** Improved Looting can share icons through its **Connect menu icons** option. Automatic sharing from Loot Filters requires the asynchronous image-provider update proposed in [Loot Filters PR #219](https://github.com/riktenx/loot-filters/pull/219). That update is still under review and is not in the currently released Loot Filters version. Ordinary menu features work without it; diagnostics report when the provider is unavailable.

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

### Ground-menu icons and loot displays

Choose **Presentation > Menu theme > OSRS** for the brown background and square bronze border used by Improved Looting's OSRS cards. **Native** is the default and keeps your existing style. The theme retains native text, icons, colors, scrolling and click positions, and is included in saved profiles and imports. Menus containing submenus, and menus already drawn by another plugin (such as Interface Styles' HD or transparent menus), keep their original appearance.

In RuneLite's **Improved Right-Clicks** settings, open **Presentation**, enable **Ground loot icons**, and choose **Native**, **Custom**, or **Both** under **Icon source**. All icon, visibility and color options stay in plugin settings. The side panel contains profiles, import/export, undo, a settings shortcut and optional diagnostics.

- **Native** shows the item's own picture and remains the default.
- **Custom** shows the selected custom icon. Hidden, unassigned or unavailable icons remain absent.
- **Both** shows custom and native icons together, avoiding duplicate Current Item icons. The native picture remains when no custom icon is available, unless **Both: respect filter visibility** is enabled.

Enable **Loot display compatibility**, then choose Custom or Both. With Improved Looting active and **Connect menu icons** enabled, its resolved settings take priority. Otherwise a compatible Loot Filters provider is used. Improved Right-Clicks caches independent images without reading another plugin's files or evaluating its rules. Existing manual assignments remain explicit overrides; previously imported filter backups remain saved and can supply icons with compatibility off.

Loot Filters still requires the upstream provider update described above. File, sprite and item images load through the active provider; no direct access to its icon directory is used. Both plugins retain their own settings.

**Use loot display in menu** optionally applies evaluated Take-row sort priority while preserving existing deprioritization and non-loot rows. **Use loot display menu colors** is a separate opt-in for item-name colors when that menu switch is enabled. Hidden-row colors retain the native/provider result. Both switches start off and preserve saved action colors and normal left-click actions.

Resolved settings follow the active filter. A `filter-reloaded` message, configuration changes, profile changes and plugin changes clear the image cache. Late results from a previous filter cannot repopulate it. The final `hidden` / `hideOverlay` values are respected; temporary ground-overlay toggles and camera visibility do not hide menu icons. When identical stacks resolve differently, no custom icon is chosen arbitrarily. Submenus retain their original text.

**Show diagnostics** reports whether a compatible provider responded. No response leaves custom icons absent; Both retains native icons according to its visibility option. See [compatibility details](LOOT-FILTERS-COMPATIBILITY.md).

The retained RS2F parser, rule evaluation helpers and preamble for previously saved imports are adapted from [riktenx/loot-filters at 993fd054](https://github.com/riktenx/loot-filters/tree/993fd054e08d42a77d3415acbb9ac7c2797726ed), under its [BSD 2-Clause license](src/main/resources/com/improvedmenus/loot-filters-LICENSE.txt). This integration changes menu presentation only; it does not render ground overlays or play sounds.

### Player badges and Copy name

Open **Player badges** in Improved Right-Clicks settings. Each feature starts off and has its own switch:

- **Friend badge:** a green smiley, distinct from the gold stars used for chat ranks. Uses the client's online-friend information.
- **Ignored badge:** a red blocked symbol for someone on your ignore list.
- **Friends Chat badge:** a blue speech bubble for members of your current Friends Chat.
- **RuneLite party badge:** purple people for your current party members.
- **Copy name:** one local menu option per player, copying their plain name to the clipboard.

Badges appear in opened player menus. Hover a decorated row to see their meaning. Existing name colors and rank icons are preserved. Matching icons (including identical images registered under another index) are not added twice. A Friends Chat rank already conveys membership, so it suppresses the extra blue bubble. Other plugins can use different artwork for the same meaning; disable our corresponding toggle if their badge overlaps semantically. Native player actions retain their order and behavior.

**Clan rank icons (info)** points to the existing controls: **Player Indicators** controls right-click rank icons, while **Chat Channels** controls chat-message icons. Improved Right-Clicks does not change those settings or add another clan-rank badge.

### Additional option indicator

Under Presentation, **Hide player option counts** excludes player actions while retaining loot and object actions underneath them. **Hide counts when Walk here** suppresses deprioritized NPC/object counts when walking is preferred; ground items and widgets remain eligible. These switches affect only the hover indicator, not the menu entries. Both start off.

**Option count anchor** offers Below left (the existing default), Below right, Above left and Above right relative to the tooltip stack. RuneLite still controls the stack position relative to the cursor. All options are saved in profiles and support import/export and undo.

## Saved settings

Settings and named profiles retain the existing `improvedmenus` storage keys. Renaming the plugin or repository does not reset them. Profiles, validated imports and the side panel's Undo tools manage plugin settings; previously saved custom icon assignments and imported filter backups remain intact after removing the sidebar editor. Imports leave other plugins' original settings and files intact.

## Compatibility

Users of the features in this table can consolidate them here after checking their settings. This is feature coverage, not a claim that every setting in each source plugin has been reproduced. Cancel/Walk/Set-heading cleanup applies only to opened menus. The extra-options indicator has its own placement controls. Useful Telegrab targets are always allowed rather than individually switchable. Hold-to-reveal is shared rather than Examine-only. Mahogany Homes cleanup preserves non-Remove actions.

Keep RuneLite's Menu Entry Swapper for swaps and specialist plugins for their remaining features. Disable overlapping filters/colors before comparing results: this plugin cannot reveal options another plugin has removed.

Menu Entry Swapper's Shift-right-click customization remains available with Examine hiding and menu colors enabled. Improved Right-Clicks keeps its native Examine lookup entries unchanged while building that menu, then applies the configured cleanup after the swap controls have been added.

Optional Improved Looting / Loot Filters compatibility uses the supported resolved-display and asynchronous image-provider interface described above. Icons decorate menu entries; they do not change ground labels, beams or sounds.

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
| [Loot Filters / Filterscape](https://github.com/riktenx/loot-filters) | Optional icon provider; adapted RS2F ingredients | Shares resolved icons and optional menu presentation through the provider interface under review. Retained parser components support previously saved imports. The provider update is required for automatic sharing. |

The [Filterscape editor](https://filterscape.xyz/) prepares scripts for Loot Filters. Edit the active filter there and load it in Loot Filters; with its compatible provider, Improved Right-Clicks follows the resolved settings without a second upload. This integration affects menu presentation, not ground labels, beams or sounds. Feature overlap does not imply complete replacement. Full licenses and adaptation details remain in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Development

Java 11 is used for the build. Run `gradlew.bat test` for automated checks or `gradlew.bat run` to launch the development client. Tests cover filtering boundaries, order/callback preservation, contexts, profiles/import validation, click origin, tooltip ownership, reveal and cosmetic restoration. See the build configuration and third-party notices for dependencies and licensing.

A short live smoke check is still needed for real menu rendering: try a dosed potion and bank item, one protected button through both click paths, a tree with/without an axe, a house menu outside/inside building mode, a colored submenu, and ground loot icons. Disable/re-enable once while a menu is open. Extensive repeated testing is not required, but automated checks do not verify actual in-game rendering.

Ground loot icons decorate the main right-click menu only. Submenus keep their original target text because RuneLite does not expose whether a submenu is open for reliable icon positioning. Reopening a menu and disabling icons restore the target text owned by Improved Right-Clicks.

Tool guards leave the knife-cut Temple Trekking swamp vines and quest Cave entrance interactions available. Ordinary axe and pickaxe protections remain independent.

## Support

[Report an issue or suggest a feature](https://github.com/wukuna/improved-right-clicks/issues). Include the affected setting, steps to reproduce, other menu plugins and a screenshot when useful. For imported icons, include a small example filter with private details removed.

## License and attribution

Improved Right-Clicks uses the [BSD 2-Clause license](LICENSE). Adapted projects, bundled RS2F components and their licenses are documented in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and the resource notices. Source attribution is separate from feature compatibility.
