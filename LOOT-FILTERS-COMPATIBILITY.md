# Loot display compatibility

Improved Right-Clicks accepts icons and optional menu presentation from **Improved Looting** through its **Connect menu icons** option. The consumer was included in [Plugin Hub update #19763](https://github.com/runelite/plugin-hub/pull/19763), merged on 30 October 2026.

The separate **Loot Filters** integration uses its [resolved-display provider](https://github.com/riktenx/loot-filters/blob/21fc0b87d58228b292e6c1e98acd3692f29aed80/plugin-messages.md), added in [PR #219](https://github.com/riktenx/loot-filters/pull/219) and merged on 12 November 2026. Use an updated Loot Filters release. In Right-Clicks, enable **Loot display compatibility** and select **Custom** or **Both** as the icon source. **Native** displays the normal item artwork.

When both providers are active, Improved Looting takes priority if its **Connect menu icons** option is enabled. Turn that option off to use Loot Filters' resolved icons and menu settings instead. Right-Clicks' compatibility status identifies the connected provider after opening a Take menu.

Custom icons, optional menu colors and loot priorities follow the active provider. Icons are displayed after loading and resized for the menu. Improved Right-Clicks keeps its own image copies without reading another plugin's files.

The `filter-reloaded` PluginMessage invalidates images even when the selected filename stays unchanged. Configuration, profile and plugin changes also invalidate images. Ordinary menus continue to work when no compatible provider responds.

Native/Custom/Both, separate menu-color controls, optional menu display settings, saved preferences and the profiles sidebar remain intact. Existing manually imported data remains stored.
