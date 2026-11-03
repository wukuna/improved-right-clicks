package com.improvedmenus;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;

@Singleton
final class LootFiltersMenuPresentation {
    @Inject private ImprovedMenusConfig config;
    @Inject private LootFiltersCompatibility compatibility;
    private final Map<MenuEntry, TargetEdit> colors = new IdentityHashMap<>();
    private Menu sortedMenu;
    private MenuEntry[] originalOrder, sortedOrder;
    private final Map<MenuEntry, Identity> identities = new IdentityHashMap<>();

    void apply(Menu menu) {
        stop();
        if(menu==null || !config.useLootFiltersInMenu()) return;
        MenuEntry[] entries=menu.getMenuEntries();
        List<Integer> slots=new ArrayList<>();
        List<Ranked> rows=new ArrayList<>();
        for(int i=0;i<entries.length;i++) {
            MenuEntry entry=entries[i];
            if(!isTake(entry) || entry.getSubMenu()!=null) continue;
            LootFiltersCompatibility.Result display=compatibility.find(entry);
            if(display==null) continue;
            slots.add(i); rows.add(new Ranked(entry,display));

            if(config.lootFiltersMenuColors() && !display.hidden && display.menuColor!=null) {
                String before=entry.getTarget();
                String after=String.format("<col=%06x>",display.menuColor.getRGB() & 0xffffff)
                    + before.replaceAll("</?col(?:=[^>]*)?>", "") + "</col>";
                if(!before.equals(after)) {
                    colors.put(entry,new TargetEdit(entry,before,after)); entry.setTarget(after);
                }
            }
        }

        rows.sort((a,b) -> {
            int priority=Boolean.compare(b.deprioritized,a.deprioritized);
            return priority!=0 ? priority : Integer.compare(a.sort,b.sort);
        });
        MenuEntry[] ordered=entries.clone();
        for(int i=0;i<slots.size();i++) ordered[slots.get(i)]=rows.get(i).entry;
        if(!sameOrder(entries,ordered)) {
            sortedMenu=menu; originalOrder=entries.clone(); sortedOrder=ordered.clone();
            for(MenuEntry entry:entries) identities.put(entry,new Identity(entry));
            menu.setMenuEntries(ordered);
        }
    }

    void stop() {
        colors.forEach((entry,edit) -> { if(edit.identity.matches(entry) && edit.after.equals(entry.getTarget())) entry.setTarget(edit.before); });
        colors.clear();
        if(sortedMenu!=null && sameOrder(sortedMenu.getMenuEntries(),sortedOrder)
                && identities.entrySet().stream().allMatch(e -> e.getValue().matches(e.getKey()))) {
            sortedMenu.setMenuEntries(originalOrder);
        }
        sortedMenu=null; originalOrder=null; sortedOrder=null; identities.clear();
    }

    static boolean isTake(MenuEntry entry) {
        MenuAction type=entry.getType();
        return (type==MenuAction.GROUND_ITEM_FIRST_OPTION || type==MenuAction.GROUND_ITEM_SECOND_OPTION
            || type==MenuAction.GROUND_ITEM_THIRD_OPTION || type==MenuAction.GROUND_ITEM_FOURTH_OPTION
            || type==MenuAction.GROUND_ITEM_FIFTH_OPTION) && "Take".equals(MenuRules.plain(entry.getOption()));
    }
    private static boolean sameOrder(MenuEntry[] a,MenuEntry[] b) {
        if(a.length!=b.length) return false;
        for(int i=0;i<a.length;i++) if(a[i]!=b[i]) return false;
        return true;
    }
    private static final class Ranked {
        final MenuEntry entry;
        final int sort;
        final boolean deprioritized;
        Ranked(MenuEntry entry,LootFiltersCompatibility.Result display) {
            this.entry=entry; sort=display.menuSort; deprioritized=entry.isDeprioritized();
        }
    }
    private static final class TargetEdit {
        final Identity identity;
        final String before,after;
        TargetEdit(MenuEntry entry,String before,String after) { identity=new Identity(entry); this.before=before; this.after=after; }
    }
    private static final class Identity {
        final MenuAction type;
        final int id,x,y,worldView;
        Identity(MenuEntry entry) { type=entry.getType(); id=entry.getIdentifier(); x=entry.getParam0(); y=entry.getParam1(); worldView=entry.getWorldViewId(); }
        boolean matches(MenuEntry entry) { return type==entry.getType() && id==entry.getIdentifier() && x==entry.getParam0() && y==entry.getParam1() && worldView==entry.getWorldViewId(); }
    }
}
