package com.improvedmenus;

import java.util.*;

/** Explicit editable keys. Profiles cannot set unrelated RuneLite configuration. */
final class SettingsCatalog
{
    static final class Setting
    {
        final String key, label, section, defaultValue;
        final String[] choices;
        Setting(String key, String label, String section, String defaultValue, String... choices)
        { this.key=key; this.label=label; this.section=section; this.defaultValue=defaultValue; this.choices=choices; }
        void validate(String value)
        {
            if (value == null) { throw new IllegalArgumentException("Missing value for " + label); }
            if (choices.length != 0 && !Arrays.asList(choices).contains(value)) { throw new IllegalArgumentException("Invalid value for " + label); }
            if (key.equals("itemRules")) { MenuRules.parseItems(value); }
            else if (key.equals("colors")) { MenuRules.parseColors(value); }
            else if (key.equals("exceptions")) { MenuRules.parseExceptions(value); }
            else if (key.equals("defaultPickerColor") || key.equals("optionCountColor"))
            { if (!value.matches("#[0-9a-fA-F]{6}")) { throw new IllegalArgumentException("Use #RRGGBB for " + label); } }
            else if (key.equals("minimumOptions"))
            {
                try { int count=Integer.parseInt(value); if (count < 1 || count > 500) { throw new NumberFormatException(); } }
                catch (NumberFormatException e) { throw new IllegalArgumentException("Use a whole number from 1 to 500 for " + label); }
            }
            else if (choices.length == 0) { MenuRules.bounded(value); }
        }
    }
    static final Map<String, Setting> ALL = new LinkedHashMap<>();
    static
    {
        add("revealKey", "Hold to reveal", "General", "CONTROL", "CONTROL", "SHIFT", "ALT", "NONE");
        bool("hideSidePanel", "Hide side panel", "General", false);
        bool("potionEmpty", "No Empty on potions", "Inventory", false);
        bool("examineInventory", "Hide item Examine", "Examine", false); bool("examineGround", "Hide ground Examine", "Examine", false);
        bool("examineNpc", "Hide NPC Examine", "Examine", false); bool("examineObject", "Hide scenery Examine", "Examine", false);
        bool("keepLoneNpc", "Keep lone NPC Examine", "Examine", true); bool("keepLoneObject", "Keep lone scenery Examine", "Examine", true);
        add("exceptions", "Always keep names (one per line)", "Rules", "");
        add("itemRules", "Item rules: action,item name", "Rules", "");
        add("colors", "Colors: target:action:#RRGGBB;", "Colors", "");
        bool("enableColors", "Enable colors", "Colors", true); bool("colorPicker", "Shift menu color editor", "Colors", true);
        bool("yellowHover", "Yellow hover highlight", "Colors", true);
        add("defaultPickerColor", "Default picker color", "Colors", "#00ff00");
        bool("chop", "Require axe", "Tools", false); bool("mine", "Require pickaxe", "Tools", false);
        bool("house", "House menu cleanup", "House", false); bool("mahogany", "Mahogany Homes cleanup", "House", false);
        add("autoRetaliate", "Auto Retaliate", "Buttons", "NORMAL", "NORMAL", "LEFT_CLICK_OFF", "DISABLED");
        add("xpDrops", "XP Drops", "Buttons", "NORMAL", "NORMAL", "LEFT_CLICK_OFF", "DISABLED");
        add("alwaysSetPlaceholders", "Bank placeholders", "Buttons", "NORMAL", "NORMAL", "LEFT_CLICK_OFF", "DISABLED");
        add("report", "Report", "Buttons", "NORMAL", "NORMAL", "LEFT_CLICK_OFF", "DISABLED");
        bool("deprioritizeLeftClickOffEntries", "Deprioritize protected buttons", "Buttons", true);
        bool("groundIcons", "Ground loot icons", "Presentation", false);
        add("menuTheme", "Menu theme", "Presentation", "NATIVE", "NATIVE", "OSRS");
        bool("lootFiltersCompatibility", "Loot Filters compatibility", "Presentation", false);
        bool("useLootFiltersInMenu", "Use Loot Filters in menu", "Presentation", false);
        bool("lootFiltersMenuColors", "Use Loot Filters menu colors", "Colors", false);
        add("groundIconSource", "Icon source", "Presentation", "NATIVE", "NATIVE", "CUSTOM", "BOTH");
        bool("bothRespectFilterVisibility", "Both: respect filter visibility", "Presentation", false);
        bool("optionCount", "Additional option count", "Presentation", false);
        add("minimumOptions", "Minimum extra options", "Presentation", "1");
        bool("countExamine", "Count Examine", "Presentation", true);
        bool("countOnWidgets", "Show count on interface widgets", "Presentation", true);
        bool("countHidePlayers", "Hide player option counts", "Presentation", false);
        bool("countHideWalkHere", "Hide counts when Walk here", "Presentation", false);
        add("countAnchor", "Option count anchor", "Presentation", "BELOW_LEFT", "BELOW_LEFT", "BELOW_RIGHT", "ABOVE_LEFT", "ABOVE_RIGHT");
        add("optionCountColor", "Option count text color", "Presentation", "#ffffff");
        bool("hideCancel", "Hide Cancel (right-click menu)", "Menu cleanup", false);
        bool("hideWalkHere", "Hide Walk here (right-click menu)", "Menu cleanup", false);
        bool("hideSetHeading", "Hide Set heading (right-click menu)", "Menu cleanup", false);
        bool("noUsePlayer", "No Use on Player", "Target protection", false);
        add("playerUseItems", "Allow items on players (comma separated)", "Target protection", TargetProtections.DEFAULT_PLAYER_ITEMS);
        bool("noTelegrabNpc", "Don't Telegrab NPCs", "Target protection", false);
        add("telegrabNpcNames", "Allow Telegrab NPC names (comma separated)", "Target protection", "");
        bool("playerFriendBadge", "Friend badge", "Player badges", false);
        bool("playerIgnoreBadge", "Ignored badge", "Player badges", false);
        bool("playerFriendsChatBadge", "Friends Chat badge", "Player badges", false);
        bool("playerPartyBadge", "RuneLite party badge", "Player badges", false);
        bool("playerCopyName", "Copy name", "Player badges", false);
        add("playerClanRankInfo", "Clan rank icons (info)", "Player badges", "Player Indicators / Chat Channels");
        bool("bankWield", "Hide Wield", "Bank", false); bool("bankWear", "Hide Wear", "Bank", false);
        bool("bankDrink", "Hide Drink", "Bank", false); bool("bankEat", "Hide Eat", "Bank", false);
    }
    private static void bool(String key, String label, String section, boolean value) { add(key,label,section,Boolean.toString(value),"false","true"); }
    static java.awt.Color colorOrDefault(String value,java.awt.Color fallback)
    { return value != null && value.matches("#[0-9a-fA-F]{6}") ? new java.awt.Color(Integer.parseInt(value.substring(1),16)) : fallback; }
    private static void add(String key, String label, String section, String value, String... choices) { ALL.put(key,new Setting(key,label,section,value,choices)); }
    private SettingsCatalog() {}
}
