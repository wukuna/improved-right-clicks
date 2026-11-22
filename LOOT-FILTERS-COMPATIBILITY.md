# Loot display compatibility

In **Presentation → Custom icon provider**, choose **Off** (default), **Improved Looting** or **Loot Filters**. Use **Custom** or **Both** as the icon source. **Native** displays the normal item artwork.

The **Loot Filters** integration uses its [resolved-display provider](https://github.com/riktenx/loot-filters/blob/21fc0b87d58228b292e6c1e98acd3692f29aed80/plugin-messages.md). Use an updated Loot Filters release. The selected provider must be enabled and have a custom icon configured for the item. A ground label's normal item picture does not necessarily mean a custom icon is configured.

Custom icons use only the selected provider. Improved Looting's **Connect menu icons** checkbox does not control this selection. An unavailable provider never silently switches to another plugin. **Off** requests no external artwork, including for users whose old compatibility checkbox was enabled. Saved manual/imported icons remain available.

Menu ordering and colours are separate opt-ins. Their existing provider selection remains unchanged: Improved Looting when its connection is enabled, otherwise Loot Filters. Choosing a custom icon provider does not enable menu effects or change either plugin's settings. Icons are displayed after loading and resized for the menu. Improved Right-Clicks keeps its own image copies without reading another plugin's files.

The `filter-reloaded` PluginMessage invalidates images even when the selected filename stays unchanged. Configuration, profile and plugin changes also invalidate images. Ordinary menus continue to work when no compatible provider responds.

Native/Custom/Both, separate menu-color controls, optional menu display settings, saved preferences and the profiles sidebar remain intact. Existing manually imported data remains stored.
