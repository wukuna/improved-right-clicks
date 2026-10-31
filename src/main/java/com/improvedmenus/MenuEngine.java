package com.improvedmenus;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

@Singleton
final class MenuEngine
{
    private static final Set<MenuAction> OBJECTS = EnumSet.of(MenuAction.GAME_OBJECT_FIRST_OPTION,
        MenuAction.GAME_OBJECT_SECOND_OPTION, MenuAction.GAME_OBJECT_THIRD_OPTION,
        MenuAction.GAME_OBJECT_FOURTH_OPTION, MenuAction.GAME_OBJECT_FIFTH_OPTION);
    private static final Set<MenuAction> ITEMS = EnumSet.of(MenuAction.CC_OP, MenuAction.CC_OP_LOW_PRIORITY,
        MenuAction.WIDGET_TARGET, MenuAction.WIDGET_TARGET_ON_WIDGET,
        MenuAction.GROUND_ITEM_FIRST_OPTION, MenuAction.GROUND_ITEM_SECOND_OPTION,
        MenuAction.GROUND_ITEM_THIRD_OPTION, MenuAction.GROUND_ITEM_FOURTH_OPTION, MenuAction.GROUND_ITEM_FIFTH_OPTION,
        MenuAction.EXAMINE_ITEM_GROUND);

    @Inject private Client client;
    @Inject private ImprovedMenusConfig config;
    @Inject private RevealState reveal;
    @Inject private TargetProtections targets;
    private List<MenuRules.Rule> itemRules = Collections.emptyList();
    private List<MenuRules.Rule> colors = Collections.emptyList();
    private List<MenuRules.Rule> exceptions = Collections.emptyList();
    private String validation = "";
    private volatile String diagnostics = "Hover an item or open a menu to preview changes.";
    private final IdentityHashMap<MenuEntry,ColorEdit> ownedColors = new IdentityHashMap<>();

    void reload()
    {
        restoreColors();
        // Invalid edits fail open instead of leaving invisible stale rules active.
        List<String> errors = new ArrayList<>();
        try { itemRules = MenuRules.parseItems(config.itemRules()); }
        catch (IllegalArgumentException e) { itemRules = Collections.emptyList(); errors.add(e.getMessage()); }
        try { colors = MenuRules.parseColors(config.colors()); }
        catch (IllegalArgumentException e) { colors = Collections.emptyList(); errors.add(e.getMessage()); }
        try { exceptions = MenuRules.parseExceptions(config.exceptions()); }
        catch (IllegalArgumentException e) { exceptions = Collections.emptyList(); errors.add(e.getMessage()); }
        validation = String.join("\n", errors);
    }
    String diagnostics() { return diagnostics; }
    void clear() { restoreColors(); diagnostics = "Menu preview cleared."; }
    private void restoreColors()
    {
        ownedColors.forEach((entry,edit) -> {
            if (entry.getOption().equals(edit.after) && MenuRules.plain(entry.getTarget()).equals(edit.target)
                && entry.getType() == edit.type && entry.getIdentifier() == edit.identifier) { entry.setOption(edit.before); }
        });
        ownedColors.clear();
    }
    void highlightHovered(Menu menu)
    {
        // Release the previous hover before finding this frame's row. Only change
        // entries whose identity and text still match our own decoration.
        ownedColors.forEach((entry,edit) -> {
            if (edit.matches(entry)) { entry.setOption(edit.colored); edit.after=edit.colored; }
        });
        if (!config.enableColors() || !config.yellowHover() || menu == null) { return; }
        MenuEntry hovered=hovered(menu);
        ColorEdit edit=ownedColors.get(hovered);
        if (edit != null && edit.matches(hovered))
        {
            // Native rendering supplies yellow. Keep image tags and target colors.
            edit.after=edit.colored.replaceAll("</?col(?:=[^>]*)?>", "");
            hovered.setOption(edit.after);
        }
    }
    private MenuEntry hovered(Menu menu)
    {
        if (menu.getMenuWidth() <= 0 || menu.getMenuHeight() <= 0) { return null; }
        MenuEntry[] entries=menu.getMenuEntries();
        // Public Menu bounds are valid only while open. The API does not expose
        // which submenu is open, so never use cached child bounds for hit testing.
        Point mouse=client.getMouseCanvasPosition();
        int x=mouse.getX(), y=mouse.getY();
        if (x <= menu.getMenuX() || x >= menu.getMenuX()+menu.getMenuWidth()
            || y <= menu.getMenuY()+18 || y >= menu.getMenuY()+menu.getMenuHeight()) { return null; }
        int scroll=client.isMenuScrollable() ? client.getMenuScroll() : 0;
        for (int i=0;i<entries.length;i++)
        {
            int baseline=menu.getMenuY()+31+(entries.length-1-i-scroll)*15;
            if (y > baseline-13 && y < baseline+3) { return entries[i]; }
        }
        return null;
    }

    void apply(Menu menu)
    { apply(menu,true); }
    void applyOpened(Menu menu)
    { apply(menu,false); }
    void groupOpened(Menu menu)
    {
        if (menu == null || !config.groupGroundItems() || reveal.isRevealed()) return;
        Map<MenuEntry, String> changed = GroundMenuGroups.collapse(menu);
        changed.forEach((entry, before) -> {
            ColorEdit edit = ownedColors.get(entry);
            if (edit != null && edit.after.equals(entry.getOption()) && edit.target.equals(MenuRules.plain(before)))
                edit.target = MenuRules.plain(entry.getTarget());
        });
        if (!changed.isEmpty()) diagnostics += "\nGrouped duplicate ground-item actions in the opened menu.";
    }
    private void apply(Menu menu,boolean capture)
    {
        if (menu == null || reveal.isRevealed()) { diagnostics = "Reveal is held: filtering paused."; return; }
        StringBuilder report = new StringBuilder(validation);
        Context context = context();
        int before=menu.getMenuEntries().length;
        apply(menu, "", context, report, 0, capture && client.isKeyPressed(KeyCode.KC_SHIFT));
        if (!capture) { cleanOpenedMenu(menu,report,0); }
        if (capture || before != menu.getMenuEntries().length)
        {
            String result = "Before: " + before + " entries. After: " + menu.getMenuEntries().length + " entries.\n"
                + (report.length() == 0 ? "No changes to this menu." : report.toString());
            diagnostics = capture ? result : diagnostics + "\nOn opening:\n" + result;
        }
    }
    void applyTargetProtections(Menu menu)
    {
        if (menu == null || reveal.isRevealed() || !client.isWidgetSelected()) { return; }
        MenuEntry[] entries = menu.getMenuEntries();
        List<MenuEntry> kept = new ArrayList<>(entries.length);
        StringBuilder report = new StringBuilder();
        for (MenuEntry entry : entries)
        {
            String reason = targets.reason(entry);
            if (reason == null) { kept.add(entry); }
            else { report.append("\nHide ").append(MenuRules.plain(entry.getOption())).append(" — ").append(reason); }
        }
        if (kept.size() != entries.length)
        {
            menu.setMenuEntries(kept.toArray(new MenuEntry[0]));
            diagnostics = "Before: " + entries.length + " entries. After: " + kept.size() + " entries.\n" + report;
        }
    }
    private void cleanOpenedMenu(Menu menu,StringBuilder report,int depth)
    {
        if (depth > 8) { return; }
        MenuEntry[] entries = menu.getMenuEntries();
        List<MenuEntry> kept = new ArrayList<>(entries.length);
        MenuEntry cancel = null;
        for (MenuEntry entry : entries)
        {
            if (entry.getSubMenu() != null) { cleanOpenedMenu(entry.getSubMenu(),report,depth+1); }
            if (entry.getType() == MenuAction.CANCEL) { cancel = entry; }
            boolean hide = entry.getType() == MenuAction.CANCEL && config.hideCancel()
                || entry.getType() == MenuAction.WALK && config.hideWalkHere()
                || entry.getType() == MenuAction.SET_HEADING && config.hideSetHeading();
            if (!hide) { kept.add(entry); }
            else if (report.length() < 12000) { report.append("\nHide ").append(MenuRules.plain(entry.getOption())).append(" — right-click menu cleanup"); }
        }
        // Never turn Cancel into a default game action or leave an empty root menu.
        if (kept.isEmpty() && cancel != null) { kept.add(cancel); report.append("\nKeep Cancel — empty-menu fallback"); }
        if (kept.size() != entries.length) { menu.setMenuEntries(kept.toArray(new MenuEntry[0])); }
    }
    private void apply(Menu menu, String inheritedTarget, Context context, StringBuilder report, int depth, boolean preserveSwapAnchors)
    {
        if (depth > 8) { return; }
        MenuEntry[] entries = menu.getMenuEntries();
        List<MenuEntry> kept = new ArrayList<>(entries.length);
        for (MenuEntry entry : entries)
        {
            // Menu Entry Swapper discovers customization targets from native Examine
            // entries on MenuOpened. Keep both their identity and exact text until
            // its handler has run; our lower-priority open handler cleans up after it.
            if (preserveSwapAnchors && isSwapAnchor(entry))
            {
                ColorEdit edit = ownedColors.remove(entry);
                if (edit != null && edit.matches(entry)) { entry.setOption(edit.before); }
                kept.add(entry);
                continue;
            }
            String target = MenuRules.plain(entry.getTarget());
            if (target.isEmpty()) { target = inheritedTarget; }
            String option = MenuRules.plain(entry.getOption());
            String reason = reason(entry, entries, target, option, context);
            if (reason != null)
            {
                if (report.length() < 12000) { report.append("\nHide ").append(option).append(" ").append(target).append(" — ").append(reason); }
                continue;
            }
            if (entry.getSubMenu() != null) { apply(entry.getSubMenu(), target, context, report, depth + 1, preserveSwapAnchors); }
            if (config.enableColors())
            {
                String color = color(target, option);
                if (color != null)
                {
                    // Strip only color tags; retain images and other plugins' presentation markers.
                    ColorEdit prior = ownedColors.get(entry);
                    String before = prior != null && prior.after.equals(entry.getOption()) ? prior.before : entry.getOption();
                    String after = "<col=" + color + ">" + entry.getOption().replaceAll("</?col(?:=[^>]*)?>", "") + "</col>";
                    if (ownedColors.size() > 1024) { ownedColors.clear(); }
                    ownedColors.put(entry,new ColorEdit(before,after,entry));
                    entry.setOption(after);
                    if (report.length() < 12000) { report.append("\nColor ").append(option).append(" ").append(target).append(" #").append(color); }
                }
            }
            kept.add(entry);
        }
        // Do not remove one entry at a time: backed entry objects can be remapped on removal.
        if (kept.size() != entries.length) { menu.setMenuEntries(kept.toArray(new MenuEntry[0])); }
    }
    private static boolean isSwapAnchor(MenuEntry entry)
    {
        MenuAction type = entry.getType();
        if (type == MenuAction.EXAMINE_OBJECT || type == MenuAction.EXAMINE_NPC) { return true; }
        int group = entry.getParam1() >>> 16;
        return (type == MenuAction.CC_OP || type == MenuAction.CC_OP_LOW_PRIORITY)
            && (group == InterfaceID.INVENTORY || group == InterfaceID.WORNITEMS)
            && entry.getIdentifier() == 10
            && MenuRules.plain(entry.getOption()).equals("Examine");
    }
    String color(String target, String option)
    {
        for (MenuRules.Rule rule : colors) { if (rule.matches(target, option)) { return rule.color; } }
        return null;
    }
    boolean excepted(String target,boolean inventory,boolean bank,boolean ground)
    {
        for (MenuRules.Rule rule : exceptions) { if (rule.applies(inventory,bank,ground) && rule.matches(target,"")) { return true; } }
        return false;
    }
    String reason(MenuEntry entry, MenuEntry[] siblings, String target, String option, Context c)
    {
        MenuAction type = entry.getType();
        String targetReason = targets.reason(entry);
        if (targetReason != null) { return targetReason; }
        boolean item = isItem(entry);
        boolean inventory = item && inventoryGroup(entry);
        boolean ground = type == MenuAction.EXAMINE_ITEM_GROUND || type == MenuAction.GROUND_ITEM_FIRST_OPTION
            || type == MenuAction.GROUND_ITEM_SECOND_OPTION || type == MenuAction.GROUND_ITEM_THIRD_OPTION
            || type == MenuAction.GROUND_ITEM_FOURTH_OPTION || type == MenuAction.GROUND_ITEM_FIFTH_OPTION;
        if (excepted(target,inventory,c.bank,ground)) { return null; }
        if (inventory && c.bank)
        {
            if (option.equalsIgnoreCase("Wield") && config.bankWield()
                || option.equalsIgnoreCase("Wear") && config.bankWear()
                || option.equalsIgnoreCase("Drink") && config.bankDrink()
                || option.equalsIgnoreCase("Eat") && config.bankEat()) { return "bank protection"; }
        }
        if (inventory && config.potionEmpty() && option.equalsIgnoreCase("Empty") && target.matches(".*\\([1-9]\\)$"))
        { return "potion Empty protection"; }
        if (item)
        {
            for (MenuRules.Rule rule : itemRules) { if (rule.applies(inventory,c.bank,ground) && rule.matches(target, option)) { return "custom item rule (" + rule.context.name().toLowerCase(Locale.ROOT) + ")"; } }
        }
        if (type == MenuAction.EXAMINE_ITEM_GROUND && config.examineGround()) { return "ground Examine"; }
        if (item && option.equalsIgnoreCase("Examine") && type != MenuAction.EXAMINE_ITEM_GROUND && config.examineInventory()) { return "item Examine"; }
        if (type == MenuAction.EXAMINE_NPC && config.examineNpc()
            && !(config.keepLoneNpc() && loneExamine(entry, siblings))) { return "NPC Examine"; }
        if (type == MenuAction.EXAMINE_OBJECT && config.examineObject()
            && !(config.keepLoneObject() && loneExamine(entry, siblings))) { return "object Examine"; }
        // Temple Trekking swamp vines use a knife, and quest cave entrances may start dialogue.
        if (type == MenuAction.GAME_OBJECT_FIRST_OPTION && c.toolsKnown)
        {
            if (config.chop() && !c.axe
                && entry.getIdentifier() != net.runelite.api.gameval.ObjectID.TEMPLETREK_SWAMPTREE_SMALL_VINES
                && (option.startsWith("Chop") && (target.contains("ree") || ToolCatalog.CHOP.contains(target))
                || option.startsWith("Cut") && (target.contains("ree") || target.equals("Sulliuscep")))) { return "axe required"; }
            if (config.mine() && !c.pick && option.equals("Mine")
                && !target.equalsIgnoreCase("Cave entrance")) { return "pickaxe required"; }
        }
        if (OBJECTS.contains(type))
        {
            if (config.house() && c.house && !c.building && HomeCatalog.OPTIONS.contains(option)) { return "house building mode off"; }
            if (config.mahogany() && option.equals("Remove") && !c.house)
            {
                Integer broken = HomeCatalog.REPAIRS.get(entry.getIdentifier());
                if (broken != null)
                {
                    ObjectComposition object = client.getObjectDefinition(entry.getIdentifier());
                    ObjectComposition active = object != null && object.getImpostorIds() != null ? object.getImpostor() : null;
                    if (active != null && active.getId() != broken) { return "furniture does not need replacement"; }
                }
            }
        }
        return null;
    }
    static boolean isItem(MenuEntry entry)
    {
        if (!ITEMS.contains(entry.getType())) { return false; }
        switch (entry.getType())
        {
            case EXAMINE_ITEM_GROUND:
            case GROUND_ITEM_FIRST_OPTION: case GROUND_ITEM_SECOND_OPTION: case GROUND_ITEM_THIRD_OPTION:
            case GROUND_ITEM_FOURTH_OPTION: case GROUND_ITEM_FIFTH_OPTION: return true;
            default: return entry.getItemId() >= 0;
        }
    }
    static boolean inventoryGroup(MenuEntry entry)
    {
        int group = entry.getParam1() >>> 16;
        return group == InterfaceID.INVENTORY || group == InterfaceID.BANKSIDE;
    }
    private static boolean loneExamine(MenuEntry entry, MenuEntry[] entries)
    {
        for (MenuEntry other : entries)
        {
            if (other != entry && other.getIdentifier() == entry.getIdentifier()
                && other.getParam0() == entry.getParam0() && other.getParam1() == entry.getParam1()
                && MenuRules.plain(other.getTarget()).equals(MenuRules.plain(entry.getTarget()))
                && other.getType() != MenuAction.CANCEL && other.getType() != MenuAction.WALK
                && !MenuRules.plain(other.getOption()).equalsIgnoreCase("Examine")) { return false; }
        }
        return true;
    }
    private Context context()
    {
        Context c = new Context();
        Widget bank = client.getWidget(InterfaceID.BANKMAIN, 0);
        c.bank = bank != null && !bank.isHidden();
        ItemContainer inv = client.getItemContainer(InventoryID.INV);
        ItemContainer worn = client.getItemContainer(InventoryID.WORN);
        c.toolsKnown = inv != null && worn != null;
        if (c.toolsKnown)
        {
            for (ItemContainer container : new ItemContainer[]{inv, worn})
            {
                for (Item item : container.getItems())
                {
                    if (item.getQuantity() > 0) { c.axe |= ToolCatalog.AXES.contains(item.getId()); c.pick |= ToolCatalog.PICKS.contains(item.getId()); }
                }
            }
        }
        if (client.getLocalPlayer() != null && client.getTopLevelWorldView() != null && client.getTopLevelWorldView().isInstance())
        {
            WorldPoint point = WorldPoint.fromLocalInstance(client.getTopLevelWorldView().getScene(), client.getLocalPlayer().getLocalLocation(), client.getTopLevelWorldView().getPlane());
            c.house = point != null && HomeCatalog.REGIONS.contains(point.getRegionID());
        }
        c.building = c.house && client.getVarbitValue(VarbitID.POH_BUILDING_MODE) != 0;
        return c;
    }
    static final class Context { boolean bank, toolsKnown, axe, pick, house, building; }
    private static final class ColorEdit
    {
        final String before, colored;
        String after, target;
        final MenuAction type;
        final int identifier;
        ColorEdit(String before,String after,MenuEntry entry)
        { this.before=before; this.after=after; this.colored=after; target=MenuRules.plain(entry.getTarget()); type=entry.getType(); identifier=entry.getIdentifier(); }
        boolean matches(MenuEntry entry)
        { return entry.getOption().equals(after) && MenuRules.plain(entry.getTarget()).equals(target)
            && entry.getType() == type && entry.getIdentifier() == identifier; }
    }
}
