package com.improvedmenus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;

/** Counts interchangeable native rows in an opened menu; never creates or invokes an action. */
final class GroundMenuGroups
{
    private static final Pattern COUNTED = Pattern.compile(".*(?: x [0-9,]+| \\(x[0-9,]+\\))$");

    /** Returns each changed row's original target for the action-color restoration guard. */
    static Map<MenuEntry, String> collapse(Menu menu)
    {
        MenuEntry[] entries = menu.getMenuEntries();
        Map<List<Object>, Group> groups = new HashMap<>();
        List<MenuEntry> kept = new ArrayList<>(entries.length);
        // The last array entry is the first displayed action. Keep that exact native row.
        for (int i = entries.length - 1; i >= 0; i--)
        {
            MenuEntry entry = entries[i];
            if (eligible(entry))
            {
                List<Object> key = Arrays.asList(entry.getType(), entry.getIdentifier(), entry.getItemId(),
                    entry.getParam0(), entry.getParam1(), entry.getWorldViewId(), entry.isDeprioritized(),
                    entry.isForceLeftClick(), entry.getOption(), entry.getTarget());
                Group group = groups.get(key);
                if (group != null)
                {
                    group.count++;
                    continue;
                }
                groups.put(key, new Group(entry));
            }
            kept.add(entry);
        }
        Map<MenuEntry, String> changed = new IdentityHashMap<>();
        if (kept.size() == entries.length) return changed;
        for (Group group : groups.values())
        {
            if (group.count <= 1) continue;
            String before = group.entry.getTarget();
            changed.put(group.entry, before);
            group.entry.setTarget(before + " (x" + group.count + ")");
        }
        Collections.reverse(kept);
        // One replacement preserves backed entry identities and relative order.
        menu.setMenuEntries(kept.toArray(new MenuEntry[0]));
        return changed;
    }

    private static boolean eligible(MenuEntry entry)
    {
        if (entry.onClick() != null || entry.getSubMenu() != null
            || COUNTED.matcher(MenuRules.plain(entry.getTarget())).matches()) return false;
        MenuAction type = entry.getType();
        return type == MenuAction.GROUND_ITEM_FIRST_OPTION || type == MenuAction.GROUND_ITEM_SECOND_OPTION
            || type == MenuAction.GROUND_ITEM_THIRD_OPTION || type == MenuAction.GROUND_ITEM_FOURTH_OPTION
            || type == MenuAction.GROUND_ITEM_FIFTH_OPTION || type == MenuAction.EXAMINE_ITEM_GROUND;
    }

    private static final class Group
    {
        final MenuEntry entry;
        int count = 1;
        Group(MenuEntry entry) { this.entry = entry; }
    }

    private GroundMenuGroups() {}
}
