package com.improvedmenus;

import net.runelite.client.config.*;

@ConfigGroup(ImprovedMenusConfig.GROUP)
public interface ImprovedMenusConfig extends Config
{
    String GROUP = "improvedmenus";
    @ConfigSection(name="General", description="Reveal controls", position=0, closedByDefault=false)
    String generalSection = "general";
    @ConfigSection(name="Hide options", description="Hide unwanted menu options and configure exceptions", position=1, closedByDefault=false)
    String hideOptionsSection = "hideOptions";
    @ConfigSection(name="Item rules", description="Item actions and name exceptions", position=2, closedByDefault=true)
    String rulesSection = "rules";
    @ConfigSection(name="Colors", description="Menu colors and the color editor", position=3, closedByDefault=true)
    String colorsSection = "colors";
    @ConfigSection(name="Tools", description="Tool requirements", position=4, closedByDefault=true)
    String toolsSection = "tools";
    @ConfigSection(name="House", description="House and Mahogany Homes cleanup", position=5, closedByDefault=true)
    String houseSection = "house";
    @ConfigSection(name="Buttons", description="Interface button protections", position=6, closedByDefault=true)
    String buttonsSection = "buttons";
    @ConfigSection(name="Presentation", description="Ground icons and hover option counts", position=7, closedByDefault=true)
    String presentationSection = "presentation";
    @ConfigSection(name="Player badges", description="Optional badges in opened player menus; hover a row for their meaning", position=8, closedByDefault=true)
    String playersSection = "players";
    @ConfigSection(name="Bank", description="Bank menu protections", position=9, closedByDefault=true)
    String bankSection = "bank";

    @ConfigItem(keyName="playerFriendBadge", name="Friend badge", description="Green smiley for friends the client identifies as online; does not reveal offline/private friends", position=0, section=playersSection)
    default boolean playerFriendBadge() { return false; }
    @ConfigItem(keyName="playerIgnoreBadge", name="Ignored badge", description="Red blocked icon for players on your ignore list", position=1, section=playersSection)
    default boolean playerIgnoreBadge() { return false; }
    @ConfigItem(keyName="playerFriendsChatBadge", name="Friends Chat badge", description="Blue chat bubble for members of your Friends Chat; omitted when an existing Friends Chat rank icon is present", position=2, section=playersSection)
    default boolean playerFriendsChatBadge() { return false; }
    @ConfigItem(keyName="playerPartyBadge", name="RuneLite party badge", description="Purple people icon for members of your current RuneLite party", position=3, section=playersSection)
    default boolean playerPartyBadge() { return false; }
    @ConfigItem(keyName="playerCopyName", name="Copy name", description="Add a local Copy name option to opened player menus. Copies the plain player name to your clipboard.", position=4, section=playersSection)
    default boolean playerCopyName() { return false; }
    @ConfigItem(keyName="playerClanRankInfo", name="Clan rank icons (info)", description="Enable or disable right-click rank icons in Player Indicators. Enable or disable chat-message icons in Chat Channels. This text is informational and does not change either plugin.", position=5, section=playersSection)
    default String playerClanRankInfo() { return "Player Indicators / Chat Channels"; }

    enum RevealKey { CONTROL, SHIFT, ALT, NONE }
    @ConfigItem(keyName="revealKey", name="Hold to reveal", description="Hold before opening a menu to reveal options hidden by Improved Right-Clicks", position=0, section=generalSection)
    default RevealKey revealKey() { return RevealKey.CONTROL; }
    @ConfigItem(keyName="hideSidePanel", name="Hide side panel", description="Hide the Improved Right-Clicks toolbar button; configure all features in RuneLite settings", position=1, section=generalSection)
    default boolean hideSidePanel() { return false; }
    @ConfigItem(keyName="potionEmpty", name="No Empty on potions", description="Hide Empty on dosed inventory potions", position=7, section=hideOptionsSection)
    default boolean potionEmpty() { return false; }
    @ConfigItem(keyName="bankWield", name="Bank: hide Wield", description="Hide inventory Wield while the bank is open", position=35, section=bankSection)
    default boolean bankWield() { return false; }
    @ConfigItem(keyName="bankWear", name="Bank: hide Wear", description="Hide inventory Wear while the bank is open", position=36, section=bankSection)
    default boolean bankWear() { return false; }
    @ConfigItem(keyName="bankDrink", name="Bank: hide Drink", description="Hide inventory Drink while the bank is open", position=37, section=bankSection)
    default boolean bankDrink() { return false; }
    @ConfigItem(keyName="bankEat", name="Bank: hide Eat", description="Hide inventory Eat while the bank is open", position=38, section=bankSection)
    default boolean bankEat() { return false; }
    @ConfigItem(keyName="examineInventory", name="Hide inventory Examine", description="Remove item Examine entries", position=8, section=hideOptionsSection)
    default boolean examineInventory() { return false; }
    @ConfigItem(keyName="examineGround", name="Hide ground Examine", description="Remove ground item Examine entries", position=9, section=hideOptionsSection)
    default boolean examineGround() { return false; }
    @ConfigItem(keyName="examineNpc", name="Hide NPC Examine", description="Remove NPC Examine entries", position=10, section=hideOptionsSection)
    default boolean examineNpc() { return false; }
    @ConfigItem(keyName="examineObject", name="Hide object Examine", description="Remove scenery Examine entries", position=11, section=hideOptionsSection)
    default boolean examineObject() { return false; }
    @ConfigItem(keyName="keepLoneNpc", name="Keep lone NPC Examine", description="Retain Examine when the NPC has no other actions", position=12, section=hideOptionsSection)
    default boolean keepLoneNpc() { return true; }
    @ConfigItem(keyName="keepLoneObject", name="Keep lone object Examine", description="Retain Examine when scenery has no other actions", position=13, section=hideOptionsSection)
    default boolean keepLoneObject() { return true; }
    @ConfigItem(keyName="exceptions", name="Always keep names", description="One name per line; * wildcard. Optional [inventory], [bank] or [ground] prefix", position=12, section=rulesSection)
    default String exceptions() { return ""; }
    @ConfigItem(keyName="itemRules", name="Item action rules", description="One action,item name per line. * wildcard. # comments. Optional [inventory], [bank], [ground] prefix. Items only", position=13, section=rulesSection)
    default String itemRules() { return ""; }
    @ConfigItem(keyName="colors", name="Menu colors", description="target:action:#RRGGBB separated by semicolons. * wildcard. Cosmetic only", position=14, section=colorsSection)
    default String colors() { return ""; }
    @ConfigItem(keyName="enableColors", name="Enable colors", description="Color supported menu options, including submenus", position=15, section=colorsSection)
    default boolean enableColors() { return true; }
    @ConfigItem(keyName="yellowHover", name="Yellow hover highlight", description="Use the native yellow highlight when hovering over a colored menu option", position=17, section=colorsSection)
    default boolean yellowHover() { return true; }
    @ConfigItem(keyName="colorPicker", name="Shift menu color editor", description="Hold Shift and open a menu to add local color controls", position=16, section=colorsSection)
    default boolean colorPicker() { return true; }
    @ConfigItem(keyName="chop", name="Require axe", description="Hide tree Chop/Cut when no axe is carried or equipped", position=17, section=toolsSection)
    default boolean chop() { return false; }
    @ConfigItem(keyName="mine", name="Require pickaxe", description="Hide Mine when no pickaxe is carried or equipped", position=18, section=toolsSection)
    default boolean mine() { return false; }
    @ConfigItem(keyName="house", name="House menu cleanup", description="Hide furniture building actions outside building mode in your house", position=19, section=houseSection)
    default boolean house() { return false; }
    @ConfigItem(keyName="mahogany", name="Mahogany Homes cleanup", description="Hide Remove on furniture that does not need replacement", position=20, section=houseSection)
    default boolean mahogany() { return false; }
    @ConfigItem(keyName="autoRetaliate", name="Auto Retaliate protection", description="Normal, right-click only or hidden", position=21, section=buttonsSection)
    default ClickMode autoRetaliate() { return ClickMode.NORMAL; }
    @ConfigItem(keyName="xpDrops", name="XP Drops protection", description="Normal, right-click only or hidden", position=22, section=buttonsSection)
    default ClickMode xpDrops() { return ClickMode.NORMAL; }
    @ConfigItem(keyName="alwaysSetPlaceholders", name="Bank placeholders protection", description="Normal, right-click only or hidden", position=23, section=bankSection)
    default ClickMode alwaysSetPlaceholders() { return ClickMode.NORMAL; }
    @ConfigItem(keyName="report", name="Report protection", description="Normal, right-click only or hidden", position=24, section=buttonsSection)
    default ClickMode report() { return ClickMode.NORMAL; }
    @ConfigItem(keyName="deprioritizeLeftClickOffEntries", name="Deprioritize protected buttons", description="Make another action or Cancel the default", position=25, section=buttonsSection)
    default boolean deprioritizeLeftClickOffEntries() { return true; }
    @ConfigItem(keyName="groundIcons", name="Ground loot icons", description="Show item icons in Take menus", position=26, section=presentationSection)
    default boolean groundIcons() { return false; }
    enum GroundIconSource
    {
        NATIVE("Native"), CUSTOM("Custom"), BOTH("Both");
        private final String label;
        GroundIconSource(String label) { this.label=label; }
        @Override public String toString() { return label; }
    }
    @ConfigItem(keyName="lootFiltersCompatibility", name="Loot Filters compatibility", description="Use resolved icons from a compatible Loot Filters version. Requires Custom or Both and its image-provider update. Colors stay separate.", position=27, section=presentationSection)
    default boolean lootFiltersCompatibility() { return false; }
    @ConfigItem(keyName="useLootFiltersInMenu", name="Use Loot Filters in menu", description="Use Loot Filters ordering and hidden-item priority for Take rows in opened menus. Does not remove actions or change left-click behavior. Icons keep their own settings; colors use the separate color option.", position=27, section=presentationSection)
    default boolean useLootFiltersInMenu() { return false; }
    @ConfigItem(keyName="lootFiltersMenuColors", name="Use Loot Filters menu colors", description="With Use Loot Filters in menu enabled, use its item-name colors in opened Take menus. Does not change your saved action colors or icon source.", position=18, section=colorsSection)
    default boolean lootFiltersMenuColors() { return false; }
    @ConfigItem(keyName="groundIconSource", name="Icon source", description="Used when Ground loot icons is enabled. Custom uses Loot Filters when compatibility is enabled, otherwise imported icons. Manual assignments take priority. Both shows custom and item icons; Custom never falls back to native icons; Both keeps native item icons when a custom icon is unavailable. Colors are controlled separately.", position=28, section=presentationSection)
    default GroundIconSource groundIconSource() { return GroundIconSource.NATIVE; }
    @ConfigItem(keyName="bothRespectFilterVisibility", name="Both: respect filter visibility", description="In Both mode, show native and custom icons only when a visible custom icon is available. Hidden, unassigned or unavailable filter icons suppress both. Manual assignments still take priority. Native mode is unaffected.", position=29, section=presentationSection)
    default boolean bothRespectFilterVisibility() { return false; }
    @ConfigItem(keyName="optionCount", name="Additional option count", description="Show the number of other available actions on hover", position=29, section=presentationSection)
    default boolean optionCount() { return false; }
    @ConfigItem(keyName="hideCancel", name="Hide Cancel", description="Hide Cancel in opened right-click menus; keep a safe fallback if nothing remains", position=1, section=hideOptionsSection)
    default boolean hideCancel() { return false; }
    @ConfigItem(keyName="hideWalkHere", name="Hide Walk here", description="Hide Walk here in opened right-click menus; normal left-click walking stays available", position=0, section=hideOptionsSection)
    default boolean hideWalkHere() { return false; }
    @ConfigItem(keyName="noUsePlayer", name="No Use on Player", description="Hide item Use on players; preserve spells and allowlisted items", position=3, section=hideOptionsSection)
    default boolean noUsePlayer() { return false; }
    @ConfigItem(keyName="playerUseItems", name="Allow items on players", description="Allowed item names, separated by commas", position=4, section=hideOptionsSection)
    default String playerUseItems() { return TargetProtections.DEFAULT_PLAYER_ITEMS; }
    @ConfigItem(keyName="noTelegrabNpc", name="Don't Telegrab NPCs", description="Hide Telekinetic Grab on NPCs, preserving known quest, training and Sailing targets", position=5, section=hideOptionsSection)
    default boolean noTelegrabNpc() { return false; }
    @ConfigItem(keyName="telegrabNpcNames", name="Allow Telegrab NPC names", description="Additional allowed NPC names, separated by commas", position=6, section=hideOptionsSection)
    default String telegrabNpcNames() { return ""; }
    @ConfigItem(keyName="hideSetHeading", name="Hide Set heading", description="Hide Set heading in opened right-click menus", position=2, section=hideOptionsSection)
    default boolean hideSetHeading() { return false; }
    @ConfigItem(keyName="minimumOptions", name="Minimum extra options", description="Only show the option count at or above this number", position=39, section=presentationSection)
    @Range(min=1,max=500)
    default int minimumOptions() { return 1; }
    @ConfigItem(keyName="countExamine", name="Count Examine", description="Include Examine in the additional-option count", position=40, section=presentationSection)
    default boolean countExamine() { return true; }
    @ConfigItem(keyName="countOnWidgets", name="Show count on interface widgets", description="Show counts on inventory, bank, shop and other interface widgets", position=41, section=presentationSection)
    default boolean countOnWidgets() { return true; }
    @ConfigItem(keyName="countHidePlayers", name="Hide player option counts", description="Exclude player actions from the hover count without hiding overlapping loot or object options", position=44, section=presentationSection)
    default boolean countHidePlayers() { return false; }
    @ConfigItem(keyName="countHideWalkHere", name="Hide counts when Walk here", description="Exclude deprioritized NPC and object options when Walk here is the preferred action; loot and interface options remain counted", position=45, section=presentationSection)
    default boolean countHideWalkHere() { return false; }
    enum CountAnchor { BELOW_LEFT, BELOW_RIGHT, ABOVE_LEFT, ABOVE_RIGHT }
    @ConfigItem(keyName="countAnchor", name="Option count anchor", description="Position relative to the hover tooltip stack; RuneLite still controls whether the stack is above or below the cursor", position=46, section=presentationSection)
    default CountAnchor countAnchor() { return CountAnchor.BELOW_LEFT; }
    @ConfigItem(keyName="optionCountColor", name="Option count text color", description="Color in #RRGGBB format", position=42, section=presentationSection)
    default String optionCountColor() { return "#ffffff"; }
    @ConfigItem(keyName="defaultPickerColor", name="Default picker color", description="Initial color for entries with no matching color rule, in #RRGGBB format", position=43, section=colorsSection)
    default String defaultPickerColor() { return "#00ff00"; }
}
