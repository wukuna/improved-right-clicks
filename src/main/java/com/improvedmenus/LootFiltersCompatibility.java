package com.improvedmenus;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

/** Optional resolved-display messages; never reads another plugin's files or implementation classes. */
@Singleton
class LootFiltersCompatibility {
    private final Client client;
    private final ImprovedMenusConfig config;
    private final PluginManager plugins;
    private final EventBus events;
    private final LootFilterImageCache images;
    private boolean collecting,enabled;
    private String namespace="loot-filters", providerName="Loot Filters";
    private final Map<Tile,Map<Integer,Result>> menuResults=new IdentityHashMap<>();
    private volatile String status="Open a Take menu to check loot display compatibility.";
    @Inject LootFiltersCompatibility(Client client,ClientThread clientThread,ImprovedMenusConfig config,PluginManager plugins,EventBus events) {
        this.client=client;this.config=config;this.plugins=plugins;this.events=events;
        images=new LootFilterImageCache(clientThread::invokeLater);
    }
    static final class Result {
        final BufferedImage image;
        final boolean itemIcon,hidden,hideOverlay;
        final int menuSort;
        final Color menuColor;
        final Map<String,Object> icon;
        final Supplier<?> provider;
        Result(BufferedImage image,boolean itemIcon) { this(image,itemIcon,false,false,0,null); }
        Result(BufferedImage image,boolean itemIcon,boolean hidden,boolean hideOverlay,int menuSort,Color menuColor) {
            this(image,itemIcon,hidden,hideOverlay,menuSort,menuColor,null,null);
        }
        Result(BufferedImage image,boolean itemIcon,boolean hidden,boolean hideOverlay,int menuSort,Color menuColor,Map<String,Object> icon,Supplier<?> provider) {
            this.image=image;this.itemIcon=itemIcon;this.hidden=hidden;this.hideOverlay=hideOverlay;
            this.menuSort=menuSort;this.menuColor=menuColor;this.icon=icon;this.provider=provider;
        }
        boolean hasIcon() { return !hidden && !hideOverlay && (image!=null || icon!=null && provider!=null); }
    }
    String status() { return status; }
    long revision() { return images.revision(); }
    void refresh() {
        if(!client.isClientThread()) return;
        boolean running=false;String next="loot-filters",name="Loot Filters";
        if(config.lootFiltersCompatibility() || config.useLootFiltersInMenu()) {
            for(Plugin plugin:plugins.getPlugins())
                if(("Loot Filters".equals(plugin.getName()) || "Loot Filters TEST".equals(plugin.getName())) && plugins.isPluginActive(plugin)) { running=true; }
            for(Plugin plugin:plugins.getPlugins())
                if(("Improved Looting".equals(plugin.getName()) || "Improved Looting TEST".equals(plugin.getName())) && plugins.isPluginActive(plugin)) { Map<String,Object> capability=new HashMap<>();events.post(new PluginMessage("improved-looting","provider-status-v1",capability));if(Boolean.TRUE.equals(capability.get("enabled"))) {running=true;next="improved-looting";name="Improved Looting";break;} }
        }
        if(running!=enabled || !next.equals(namespace)) { images.clear();endMenu();enabled=running;namespace=next;providerName=name; }
        if(!config.lootFiltersCompatibility() && !config.useLootFiltersInMenu()) status="Loot display compatibility is off.";
        else if(!running) status="Enable Loot Filters or Improved Looting to use its resolved display settings.";
    }
    void reset() { endMenu();images.clear();enabled=false;status="Open a Take menu to check loot display compatibility."; }
    void beginMenu() { refresh();menuResults.clear();collecting=true; }
    void endMenu() { menuResults.clear();collecting=false; }
    Result findIcon(MenuEntry entry) { Result result=find(entry);image(result);return result; }
    BufferedImage image(Result result) {
        if(result==null || !result.hasIcon() || !client.isClientThread()) return null;
        if(result.image!=null) return result.image;
        return images.get(result.icon,result.provider);
    }
    Result find(MenuEntry entry) {
        if(!enabled || !client.isClientThread()) return null;
        WorldView view=client.getWorldView(entry.getWorldViewId());
        if(view==null || view.getScene()==null) return null;
        Tile[][][] tiles=view.getScene().getTiles();
        int plane=view.getPlane(),x=entry.getParam0(),y=entry.getParam1();
        if(tiles==null || plane<0 || plane>=tiles.length || tiles[plane]==null || x<0 || x>=tiles[plane].length
            || tiles[plane][x]==null || y<0 || y>=tiles[plane][x].length || tiles[plane][x][y]==null) return null;
        if(!collecting) return request(view,x,y,plane,entry.getIdentifier());
        Map<Integer,Result> results=menuResults.computeIfAbsent(tiles[plane][x][y],ignored->new HashMap<>());
        if(!results.containsKey(entry.getIdentifier())) results.put(entry.getIdentifier(),request(view,x,y,plane,entry.getIdentifier()));
        return results.get(entry.getIdentifier());
    }
    Result request(WorldView view,int x,int y,int plane,int itemId) {
        Map<String,Object> data=new HashMap<>();
        data.put("worldViewId",view.getId());data.put("x",view.getBaseX()+x);data.put("y",view.getBaseY()+y);data.put("z",plane);data.put("itemId",itemId);
        events.post(new PluginMessage(namespace,"resolved-display-request-v1",data));
        Result result=decode(data,itemId);
        status=result!=null?"Connected to "+providerName+". Icons, menu behavior and colors use their separate settings.":
            data.containsKey("status")?providerName+" has no ready, unambiguous display settings for this pile.":
            "This "+providerName+" version does not provide resolved display settings. A compatible provider update is required.";
        return result;
    }
    static Result decode(Map<String,Object> data,int itemId) {
        if(!"ok".equals(data.get("status")) || !(data.get("display") instanceof Map)) return null;
        Map<?,?> display=(Map<?,?>)data.get("display");
        if(!(display.get("hidden") instanceof Boolean) || !(display.get("hideOverlay") instanceof Boolean)
            || !(display.get("menuSort") instanceof Integer) || display.get("menuTextColor")!=null && !(display.get("menuTextColor") instanceof Color)) return null;
        Map<String,Object> descriptor=null;Supplier<?> provider=null;boolean same=false;
        if(display.get("icon")!=null) {
            if(!(display.get("icon") instanceof Map)) return null;
            Map<?,?> icon=(Map<?,?>)display.get("icon");
            Object type=icon.get("type");if(!(type instanceof String)) return null;
            if(icon.get("getImage") instanceof Supplier) provider=(Supplier<?>)icon.get("getImage");
            descriptor=new HashMap<>();descriptor.put("type",type);
            for(String key:Arrays.asList("id","index","quantity","height","filename")) {
                Object value=icon.get(key);
                if(value!=null && !(value instanceof Integer) && !(key.equals("filename") && value instanceof String)) return null;
                if(value!=null) descriptor.put(key,value);
            }
            descriptor=Collections.unmodifiableMap(descriptor);
            same=("item".equals(type) || "current-item".equals(type)) && Integer.valueOf(itemId).equals(icon.get("id"));
        }
        return new Result(null,same,(Boolean)display.get("hidden"),(Boolean)display.get("hideOverlay"),
            (Integer)display.get("menuSort"),(Color)display.get("menuTextColor"),descriptor,provider);
    }
}
