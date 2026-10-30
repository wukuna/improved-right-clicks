# Loot display compatibility

Improved Right-Clicks accepts icons and optional menu presentation from **Improved Looting** through its **Connect menu icons** option. The consumer was included in [Plugin Hub update #19763](https://github.com/runelite/plugin-hub/pull/19763), merged on 30 October 2026.

The separate **Loot Filters** integration requires the provider proposed in [Loot Filters PR #219](https://github.com/riktenx/loot-filters/pull/219). As of 30 October 2026, that PR is still under review; the regular Loot Filters release does not expose this interface. Improved Right-Clicks remains usable without it. When both compatible providers respond, Improved Looting takes priority.

The consumer requests resolved display settings on the client thread using a mutable PluginMessage map. It obtains independent images through the icon descriptor's `getImage` supplier of `CompletableFuture<BufferedImage>`. It never reads another plugin's files or shares Loot Filters' cached images. Image completion is handled on the client thread; cache entries are bounded and stale completions are discarded after invalidation.

The `filter-reloaded` PluginMessage invalidates images even when the selected filename stays unchanged. Configuration, profile and plugin changes also invalidate images. Ordinary menus continue to work when no compatible provider responds.

Native/Custom/Both, separate menu-color controls, optional menu display settings, saved preferences and the profiles sidebar remain intact. Existing manually imported data remains stored. The proposed Loot Filters interface remains subject to its maintainers' review.
