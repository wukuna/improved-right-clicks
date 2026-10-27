/*
 * Adapted portions from Ground Loot Icons; original copyright Louis Hong.
 *
 * BSD 2-Clause License
 * 
 * Copyright (c) 2024, Louis Hong
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.improvedmenus;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

/** Icon row geometry adapted from Fiffers/ground-loot-icons; see bundled license. */
@Singleton
final class MenuPresentation extends Overlay
{
    private static final Set<MenuAction> GROUND = EnumSet.of(MenuAction.GROUND_ITEM_FIRST_OPTION,MenuAction.GROUND_ITEM_SECOND_OPTION,
        MenuAction.GROUND_ITEM_THIRD_OPTION,MenuAction.GROUND_ITEM_FOURTH_OPTION,MenuAction.GROUND_ITEM_FIFTH_OPTION);
    @Inject private Client client;
    @Inject private ImprovedMenusConfig config;
    @Inject private ItemManager items;
    @Inject private ChatIconManager icons;
    @Inject private TooltipManager tooltips;
    @Inject private net.runelite.client.config.RuneLiteConfig runeLiteConfig;
    private int placeholder = -1;
    private ItemIcons itemIcons;
    void setItemIcons(ItemIcons library) { itemIcons=library; }
    private final IdentityHashMap<MenuEntry,TargetEdit> ownedTargets = new IdentityHashMap<>();
    private String marker = "";

    MenuPresentation() { setPosition(OverlayPosition.DYNAMIC); setLayer(OverlayLayer.ALWAYS_ON_TOP); }
    void start()
    {
        // ChatIconManager has no removal API. Reuse this registration on re-enable.
        if (placeholder == -1) { placeholder=icons.registerChatIcon(new BufferedImage(17,15,BufferedImage.TYPE_INT_ARGB)); }
    }
    void stop()
    {
        // Restore a decorated open menu; do not modify entries remapped since it was built.
        ownedTargets.forEach((entry,edit) -> { if (edit.matches(entry)) { entry.setTarget(edit.original); } });
        ownedTargets.clear();
    }
    void decorate(Menu menu)
    {
        stop();
        if (!config.groundIcons()) { return; }
        int index=icons.chatIconIndex(placeholder); if (index == -1) { return; }
        marker="<img=" + index + ">";
        // Submenu bounds are not valid unless open, and the API does not expose
        // that state. Keep submenu text native rather than reserving blank icons.
        if (menu == null) { return; }
        for (MenuEntry entry : menu.getMenuEntries())
        {
            if (GROUND.contains(entry.getType()) && "Take".equals(MenuRules.plain(entry.getOption())))
            {
                String original=entry.getTarget();
                ImprovedMenusConfig.GroundIconSource source=config.groundIconSource();
                ItemIcons.Assignment custom=source==ImprovedMenusConfig.GroundIconSource.NATIVE || source==null || itemIcons==null ? null : itemIcons.find(entry.getIdentifier());
                if(custom==null && source!=null && source!=ImprovedMenusConfig.GroundIconSource.NATIVE && itemIcons!=null && itemIcons.filters!=null) { custom=itemIcons.filters.find(entry); }
                if((source==ImprovedMenusConfig.GroundIconSource.CUSTOM || source==ImprovedMenusConfig.GroundIconSource.BOTH && config.bothRespectFilterVisibility()) && custom==null) { continue; }
                boolean both=source==ImprovedMenusConfig.GroundIconSource.BOTH && custom!=null
                    && custom.kind!=ItemIcons.Kind.CURRENT_ITEM && !(custom.kind==ItemIcons.Kind.ITEM && custom.iconId==entry.getIdentifier());
                String decorated = marker+(both ? marker : "")+original;
                TargetEdit edit=new TargetEdit(entry,original,decorated);
                edit.custom=custom; edit.both=both; edit.nativeAllowed=source!=ImprovedMenusConfig.GroundIconSource.CUSTOM && !(source==ImprovedMenusConfig.GroundIconSource.BOTH && config.bothRespectFilterVisibility());
                if (decorated.equals(original)) { continue; }
                ownedTargets.put(entry,edit);
                entry.setTarget(decorated);
            }
        }
    }
    @Override public Dimension render(Graphics2D graphics)
    {
        if (client.isMenuOpen())
        {
            if (config.groundIcons()) { draw(graphics,client.getMenu()); }
        }
        return null;
    }
    void queueOptionCount()
    {
        if (!client.isMenuOpen() && config.optionCount())
        {
            int count=additionalCount(client.getMenu().getMenuEntries(),config.countExamine(),config.countOnWidgets(),config.countHidePlayers(),config.countHideWalkHere());
            if (count >= Math.max(1,config.minimumOptions()) && client.getMouseCanvasPosition().getX() >= 0)
            {
                java.awt.Color color=SettingsCatalog.colorOrDefault(config.optionCountColor(),java.awt.Color.WHITE);
                String text=String.format("<col=%06x>+%d options</col>",color.getRGB() & 0xffffff,count);
                ImprovedMenusConfig.CountAnchor anchor=config.countAnchor();
                Tooltip tooltip=anchor==ImprovedMenusConfig.CountAnchor.BELOW_RIGHT || anchor==ImprovedMenusConfig.CountAnchor.ABOVE_RIGHT
                    ? new Tooltip(new OptionCountTooltip(text,tooltips,client,runeLiteConfig.overlayBackgroundColor())) : new Tooltip(text);
                if (anchor==ImprovedMenusConfig.CountAnchor.ABOVE_LEFT || anchor==ImprovedMenusConfig.CountAnchor.ABOVE_RIGHT) { tooltips.addFront(tooltip); }
                else { tooltips.add(tooltip); }
            }
        }
    }
    static int additionalCount(MenuEntry[] entries)
    { return additionalCount(entries,true,true); }
    static int additionalCount(MenuEntry[] entries,boolean countExamine,boolean countOnWidgets)
    { return additionalCount(entries,countExamine,countOnWidgets,false,false); }
    static int additionalCount(MenuEntry[] entries,boolean countExamine,boolean countOnWidgets,boolean hidePlayers,boolean hideWalkHere)
    {
        MenuEntry primary=entries.length==0 ? null : entries[entries.length-1];
        // Ignore players only when choosing whether the remaining world target is Walk here.
        MenuEntry worldPrimary=primary;
        if (hidePlayers)
        {
            worldPrimary=null;
            for (int i=entries.length-1;i>=0;i--)
            {
                if (!isPlayer(entries[i].getType()) && entries[i].getType()!=MenuAction.CANCEL)
                { worldPrimary=entries[i]; break; }
            }
        }
        boolean walking=hideWalkHere && worldPrimary!=null && worldPrimary.getType()==MenuAction.WALK;
        int count=0;
        boolean primaryCounted=false;
        for (MenuEntry entry : entries)
        {
            MenuAction type=entry.getType();
            if (hidePlayers && isPlayer(type)) { continue; }
            if (walking && isNpcOrObject(type)) { continue; }
            if (!countOnWidgets && entry.getWidget() != null) { continue; }
            if (!countExamine && (type == MenuAction.EXAMINE_NPC || type == MenuAction.EXAMINE_OBJECT
                || type == MenuAction.EXAMINE_ITEM_GROUND || MenuRules.plain(entry.getOption()).equalsIgnoreCase("Examine"))) { continue; }
            if (type != MenuAction.CANCEL && type != MenuAction.WALK && type != MenuAction.WIDGET_CONTINUE
                && type != MenuAction.RUNELITE && type != MenuAction.RUNELITE_OVERLAY && type != MenuAction.RUNELITE_OVERLAY_CONFIG)
            { count++; if (entry==primary) { primaryCounted=true; } }
        }
        // Preserve the original count when the new filters are off. With filters on,
        // only subtract the actual default action if it survived the filtering.
        return Math.max(0,count-(!hidePlayers && !hideWalkHere || primaryCounted ? 1 : 0));
    }
    private static boolean isPlayer(MenuAction type)
    {
        return type==MenuAction.WIDGET_TARGET_ON_PLAYER || type==MenuAction.ITEM_USE_ON_PLAYER || type==MenuAction.RUNELITE_PLAYER
            || type==MenuAction.PLAYER_FIRST_OPTION || type==MenuAction.PLAYER_SECOND_OPTION || type==MenuAction.PLAYER_THIRD_OPTION
            || type==MenuAction.PLAYER_FOURTH_OPTION || type==MenuAction.PLAYER_FIFTH_OPTION || type==MenuAction.PLAYER_SIXTH_OPTION
            || type==MenuAction.PLAYER_SEVENTH_OPTION || type==MenuAction.PLAYER_EIGHTH_OPTION;
    }
    private static boolean isNpcOrObject(MenuAction type)
    {
        return type==MenuAction.NPC_FIRST_OPTION || type==MenuAction.NPC_SECOND_OPTION || type==MenuAction.NPC_THIRD_OPTION
            || type==MenuAction.NPC_FOURTH_OPTION || type==MenuAction.NPC_FIFTH_OPTION || type==MenuAction.EXAMINE_NPC
            || type==MenuAction.ITEM_USE_ON_NPC || type==MenuAction.WIDGET_TARGET_ON_NPC
            || type==MenuAction.GAME_OBJECT_FIRST_OPTION || type==MenuAction.GAME_OBJECT_SECOND_OPTION
            || type==MenuAction.GAME_OBJECT_THIRD_OPTION || type==MenuAction.GAME_OBJECT_FOURTH_OPTION
            || type==MenuAction.GAME_OBJECT_FIFTH_OPTION || type==MenuAction.EXAMINE_OBJECT
            || type==MenuAction.ITEM_USE_ON_GAME_OBJECT || type==MenuAction.WIDGET_TARGET_ON_GAME_OBJECT;
    }
    private void draw(Graphics2D g,Menu menu)
    {
        if (menu == null || menu.getMenuWidth() <= 0 || menu.getMenuHeight() <= 0) { return; }
        MenuEntry[] entries=menu.getMenuEntries();
        int scroll=client.isMenuScrollable() ? client.getMenuScroll() : 0;
        FontMetrics metrics=g.getFontMetrics(FontManager.getRunescapeFont());
        for (int i=0;i<entries.length;i++)
        {
            MenuEntry entry=entries[i];
            TargetEdit edit=ownedTargets.get(entry);
            if (edit==null || !edit.matches(entry) || !entry.getTarget().startsWith(marker)) { continue; }
            int y=menu.getMenuY()+19+(entries.length-1-i-scroll)*15;
            if (y < menu.getMenuY()+19 || y+15 > menu.getMenuY()+menu.getMenuHeight()) { continue; }
            int x=menu.getMenuX()+8+metrics.stringWidth(MenuRules.plain(entry.getOption())+" ");
            BufferedImage custom=edit.custom==null || itemIcons==null ? null : itemIcons.image(edit.custom,entry.getIdentifier());
            if(custom!=null) { g.drawImage(custom,x,y,17,15,null); }
            else if(edit.nativeAllowed) { g.drawImage(items.getImage(entry.getIdentifier(),Integer.MAX_VALUE,false),x,y,17,15,null); }
            if(edit.both && custom!=null) { g.drawImage(items.getImage(entry.getIdentifier(),Integer.MAX_VALUE,false),x+17,y,17,15,null); }
        }
    }
    private static final class TargetEdit
    {
        ItemIcons.Assignment custom; boolean both, nativeAllowed;
        final String original, decorated;
        final MenuAction type;
        final int identifier, param0, param1;
        TargetEdit(MenuEntry entry,String original,String decorated)
        {
            this.original=original; this.decorated=decorated; type=entry.getType();
            identifier=entry.getIdentifier(); param0=entry.getParam0(); param1=entry.getParam1();
        }
        boolean matches(MenuEntry entry)
        {
            return entry.getTarget().equals(decorated) && entry.getType()==type
                && entry.getIdentifier()==identifier && entry.getParam0()==param0 && entry.getParam1()==param1;
        }
    }
}
