# Loot Filters compatibility

Companion implementation for [Loot Filters PR #219](https://github.com/riktenx/loot-filters/pull/219), currently under review. The regular Loot Filters release does not yet expose this interface. This consumer must be reviewed and coordinated with the provider release.

The consumer requests resolved display settings on the client thread using a mutable PluginMessage map. It obtains independent images through the icon descriptor's `getImage` supplier of `CompletableFuture<BufferedImage>`. It never reads another plugin's files or shares Loot Filters' cached images. Image completion is handled on the client thread; cache entries are bounded and stale completions are discarded after invalidation.

The `filter-reloaded` PluginMessage invalidates images even when the selected filename stays unchanged. Configuration, profile and plugin changes also invalidate images. Ordinary menus continue to work when no compatible provider responds.

Native/Custom/Both, separate menu-color controls, optional menu display settings, saved preferences and the profiles sidebar remain intact. Existing manually imported data remains stored. This interface's exact naming remains subject to maintainer review.
