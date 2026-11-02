package com.improvedmenus;

import com.google.gson.*;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

@Singleton
final class ProfileStore
{
    @Inject private ConfigManager manager;
    @Inject private Gson gson;
    private final Deque<Edit> undo = new ArrayDeque<>();
    static final String PROFILE_KEY = "savedProfiles";
    static final int MAX_JSON = 100000;

    String value(SettingsCatalog.Setting setting)
    {
        String value = manager.getConfiguration(ImprovedMenusConfig.GROUP, setting.key);
        return value == null ? setting.defaultValue : value;
    }
    Map<String,String> snapshot()
    {
        Map<String,String> result = new LinkedHashMap<>();
        for (SettingsCatalog.Setting setting : SettingsCatalog.ALL.values()) { result.put(setting.key, value(setting)); }
        return result;
    }
    synchronized void apply(Map<String,String> settings)
    {
        validate(settings);
        Map<String,String> before = new LinkedHashMap<>();
        Map<String,String> after = new LinkedHashMap<>();
        settings.forEach((key,next) -> {
            String current = value(SettingsCatalog.ALL.get(key));
            if (!current.equals(next)) { before.put(key,current); after.put(key,next); }
        });
        if (after.isEmpty()) { return; }
        undo.addLast(new Edit(before,after));
        if (undo.size() > 20) { undo.removeFirst(); }
        write(after);
    }
    private void write(Map<String,String> settings)
    {
        settings.forEach((key,value) -> manager.setConfiguration(ImprovedMenusConfig.GROUP,key,value));
    }
    synchronized boolean undo()
    {
        if (undo.isEmpty()) { return false; }
        Edit edit = undo.removeLast();
        Map<String,String> restore = new LinkedHashMap<>();
        edit.before.forEach((key,previous) -> {
            // Preserve subsequent edits made through RuneLite or another plugin.
            if (value(SettingsCatalog.ALL.get(key)).equals(edit.after.get(key))) { restore.put(key,previous); }
        });
        write(restore); return true;
    }
    private static final class Edit
    {
        final Map<String,String> before;
        final Map<String,String> after;
        Edit(Map<String,String> before,Map<String,String> after) { this.before=before; this.after=after; }
    }
    synchronized void clearHistory() { undo.clear(); }
    static void validate(Map<String,String> settings)
    {
        if (settings == null || settings.size() > SettingsCatalog.ALL.size()) { throw new IllegalArgumentException("Invalid settings"); }
        settings.forEach((key,value) -> {
            SettingsCatalog.Setting s = SettingsCatalog.ALL.get(key);
            if (s == null) { throw new IllegalArgumentException("Unknown setting: " + key); }
            s.validate(value);
        });
    }
    String exportSettings(Map<String,String> settings)
    {
        validate(settings);
        JsonObject root = new JsonObject(); root.addProperty("version",1);
        root.add("settings",gson.toJsonTree(settings));
        return gson.toJson(root);
    }
    Map<String,String> previewImport(String json)
    {
        if (json == null || json.length() > MAX_JSON) { throw new IllegalArgumentException("Import is too large"); }
        try
        {
            JsonObject root = gson.fromJson(json, JsonObject.class);
            if (root == null || root.size() != 2 || !root.has("version")
                || !root.get("version").isJsonPrimitive() || !root.getAsJsonPrimitive("version").isNumber()
                || root.get("version").getAsBigDecimal().compareTo(java.math.BigDecimal.ONE) != 0
                || !root.has("settings") || !root.get("settings").isJsonObject())
            { throw new IllegalArgumentException("Unsupported profile version or format"); }
            JsonObject settings = root.getAsJsonObject("settings");
            Map<String,String> result = new LinkedHashMap<>();
            for (Map.Entry<String,JsonElement> entry : settings.entrySet())
            {
                if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString())
                { throw new IllegalArgumentException("Setting values must be strings"); }
                result.put(entry.getKey(),entry.getValue().getAsString());
            }
            validate(result); return result;
        }
        catch (JsonParseException | IllegalStateException | ClassCastException | NumberFormatException e) { throw new IllegalArgumentException("Invalid profile JSON",e); }
    }
    private JsonObject profiles()
    {
        String raw = manager.getConfiguration(ImprovedMenusConfig.GROUP, PROFILE_KEY);
        if (raw == null) { return new JsonObject(); }
        try
        {
            if (raw.length() > MAX_JSON * 20) { return new JsonObject(); }
            JsonObject result = gson.fromJson(raw, JsonObject.class);
            return result == null ? new JsonObject() : result;
        }
        catch (JsonParseException e) { return new JsonObject(); }
    }
    List<String> names() { return new ArrayList<>(profiles().keySet()); }
    synchronized void save(String name)
    {
        name = name.trim();
        if (name.isEmpty() || name.length() > 40) { throw new IllegalArgumentException("Use a profile name of 1–40 characters"); }
        JsonObject profiles = profiles();
        if (profiles.size() >= 20 && !profiles.has(name)) { throw new IllegalArgumentException("At most 20 profiles are allowed"); }
        profiles.addProperty(name,exportSettings(snapshot()));
        manager.setConfiguration(ImprovedMenusConfig.GROUP,PROFILE_KEY,gson.toJson(profiles));
    }
    Map<String,String> load(String name)
    {
        JsonElement profile = profiles().get(name);
        if (profile == null || !profile.isJsonPrimitive()) { throw new IllegalArgumentException("Profile not found"); }
        return previewImport(profile.getAsString());
    }
    Map<String,String> legacyPreview(String source)
    {
        Map<String,String> result = new LinkedHashMap<>();
        switch (source)
        {
            case "No Examine":
                legacy(result,"noexamine","examineItemGround","examineGround");
                legacy(result,"noexamine","examineItemsInventory","examineInventory");
                legacy(result,"noexamine","examineNpcs","examineNpc");
                legacy(result,"noexamine","examineObjects","examineObject");
                legacy(result,"noexamine","keepLonelyExamineNpcs","keepLoneNpc");
                legacy(result,"noexamine","keepLonelyExamineObjects","keepLoneObject");
                legacy(result,"noexamine","cancelEverywhere","hideCancel");
                legacy(result,"noexamine","walkHereEverywhere","hideWalkHere");
                legacy(result,"noexamine","setHeadingEverywhere","hideSetHeading"); break;
            case "No Use Player": legacy(result,"no-use-player","whitelistItemsString","playerUseItems"); break;
            case "Don't Telegrab NPCs": legacy(result,"donttelegrabnpcs","telegrabWhitelist","telegrabNpcNames"); break;
            case "No Misclicks In Bank":
                legacy(result,"nowieldinbank","hideWield","bankWield"); legacy(result,"nowieldinbank","hideWear","bankWear");
                legacy(result,"nowieldinbank","hideDrink","bankDrink"); legacy(result,"nowieldinbank","hideEat","bankEat"); break;
            case "Item Option Hider": legacy(result,"itemoptionhider","hides","itemRules"); break;
            case "Menu Colorizer":
                legacy(result,"menucolorizer","colorMappings","colors"); legacy(result,"menucolorizer","enableColoring","enableColors");
                legacy(result,"menucolorizer","showTagOptions","colorPicker");
                legacyColor(result,"menucolorizer","defaultPickerColor","defaultPickerColor"); break;
            case "Mouseover Options+":
                legacy(result,"mouseoveroptions","minimumOptions","minimumOptions");
                legacy(result,"mouseoveroptions","includeExamine","countExamine");
                legacy(result,"mouseoveroptions","showOnWidgets","countOnWidgets");
                legacyColor(result,"mouseoveroptions","textColor","optionCountColor"); break;
            case "Tool Required": legacy(result,"tool-required","chopDown","chop"); legacy(result,"tool-required","mine","mine"); break;
            case "Misclick Guard":
                for (String key : Arrays.asList("autoRetaliate","xpDrops","alwaysSetPlaceholders","report","deprioritizeLeftClickOffEntries"))
                { legacy(result,"misclick-guard",key,key); } break;
            default: throw new IllegalArgumentException("Select a supported source");
        }
        validate(result); return result;
    }
    private void legacy(Map<String,String> result,String group,String oldKey,String key)
    {
        String raw = manager.getConfiguration(group,oldKey);
        if (raw != null) { result.put(key,raw); }
    }
    private void legacyColor(Map<String,String> result,String group,String oldKey,String key)
    {
        String raw=manager.getConfiguration(group,oldKey);
        if (raw == null) { return; }
        try { result.put(key,String.format("#%06x",java.awt.Color.decode(raw).getRGB() & 0xffffff)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid saved color for " + oldKey); }
    }
}
