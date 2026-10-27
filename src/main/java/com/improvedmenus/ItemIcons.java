package com.improvedmenus;

import com.google.gson.Gson;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;

/** Our own assignments; never writes Loot Filters configuration or files. */
@Singleton
class ItemIcons
{
    static final String KEY="itemIconAssignments", BACKUP="itemIconBackup";
    enum Kind { HEART, STAR, DIAMOND, CURRENT_ITEM, ITEM, SPRITE, IMAGE }
    static final class Assignment
    {
        String image; String name; int itemId=-1; Kind kind; int iconId; int spriteIndex;
        String key() { return itemId>=0 ? "id:"+itemId : "name:"+name.toLowerCase(Locale.ROOT); }
        @Override public String toString() { return (itemId>=0 ? "Item #"+itemId : name)+" — "+kind+(kind==Kind.ITEM || kind==Kind.SPRITE ? " #"+iconId : ""); }
    }
    @Inject ConfigManager configs;
    @Inject Gson gson;
    @Inject ItemManager items;
    @Inject SpriteManager sprites;
    @Inject FilterIcons filters;
    private final Map<String,BufferedImage> imageCache=new LinkedHashMap<>();
    private final Map<Kind,BufferedImage> builtins=new EnumMap<>(Kind.class);
    private String cached;
    private List<Assignment> assignments=Collections.emptyList();
    synchronized List<Assignment> snapshot()
    {
        String raw=configs.getConfiguration(ImprovedMenusConfig.GROUP,KEY);
        if(!Objects.equals(raw,cached))
        {
            try { assignments=parse(raw==null ? "[]" : raw); }
            catch(IllegalArgumentException e) { assignments=Collections.emptyList(); }
            cached=raw;
        }
        return new ArrayList<>(assignments);
    }
    List<Assignment> parse(String raw)
    {
        if(raw==null || raw.length()>200000) { throw new IllegalArgumentException("Icon data is too large."); }
        try
        {
            Assignment[] data=gson.fromJson(raw,Assignment[].class);
            if(data==null || data.length>500) { throw new IllegalArgumentException("Use at most 500 assignments."); }
            LinkedHashMap<String,Assignment> unique=new LinkedHashMap<>();
            for(Assignment a:data)
            {
                if(a==null || a.kind==null || a.itemId < -1 || a.itemId>100000 || a.iconId<0 || a.iconId>100000 || a.spriteIndex<0 || a.spriteIndex>255
                    || (a.itemId<0 && (a.name==null || a.name.trim().isEmpty() || a.name.length()>120 || a.name.contains("<") || a.name.contains(">"))))
                { throw new IllegalArgumentException("Invalid icon assignment."); }
                if(a.kind==Kind.IMAGE) { IconImages.decode(a.image); }
                if(a.name!=null) { a.name=a.name.trim(); }
                unique.put(a.key(),a);
            }
            return new ArrayList<>(unique.values());
        }
        catch(com.google.gson.JsonParseException e) { throw new IllegalArgumentException("Invalid icon JSON."); }
    }
    String export() { return gson.toJson(snapshot()); }
    void save(List<Assignment> next)
    {
        String json=gson.toJson(next); parse(json);
        configs.setConfiguration(ImprovedMenusConfig.GROUP,BACKUP,export());
        configs.setConfiguration(ImprovedMenusConfig.GROUP,KEY,json);
    }
    void undo()
    {
        String before=configs.getConfiguration(ImprovedMenusConfig.GROUP,BACKUP);
        if(before!=null) { save(parse(before)); }
    }
    Assignment find(int id)
    {
        List<Assignment> entries=snapshot();
        for(Assignment a:entries) { if(a.itemId==id) { return a; } }
        if(entries.isEmpty()) { return null; }
        String name=items.getItemComposition(id).getName();
        for(Assignment a:entries) { if(a.itemId<0 && a.name.equalsIgnoreCase(name)) { return a; } }
        return null;
    }
    BufferedImage image(Assignment a,int id)
    {
        if(a==null) { return null; }
        if(a.kind==Kind.CURRENT_ITEM || a.kind==Kind.ITEM) { return items.getImage(a.kind==Kind.CURRENT_ITEM ? id : a.iconId); }
        if(a.kind==Kind.IMAGE) {
            if(imageCache.size()>256) imageCache.clear();
            return imageCache.computeIfAbsent(a.image,IconImages::decode);
        }
        if(a.kind==Kind.SPRITE) { try { return sprites.getSprite(a.iconId,a.spriteIndex); } catch(IndexOutOfBoundsException e) { return null; } }
        if(builtins.containsKey(a.kind)) { return builtins.get(a.kind); }
        BufferedImage result=new BufferedImage(17,15,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=result.createGraphics();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            if(a.kind==Kind.HEART) { g.setColor(new Color(255,105,110)); g.fillOval(1,1,8,8); g.fillOval(8,1,8,8); g.fillPolygon(new int[]{1,16,8},new int[]{6,6,14},3); }
            else if(a.kind==Kind.DIAMOND) { g.setColor(Color.CYAN); g.fillPolygon(new int[]{8,15,8,1},new int[]{0,7,14,7},4); }
            else { g.setColor(new Color(255,210,60)); Polygon star=new Polygon(); for(int i=0;i<10;i++) { double angle=-Math.PI/2+i*Math.PI/5; double radius=i%2==0 ? 7 : 3; star.addPoint(8+(int)(Math.cos(angle)*radius),7+(int)(Math.sin(angle)*radius)); } g.fillPolygon(star); }
        }
        finally { g.dispose(); }
        builtins.put(a.kind,result);
        return result;
    }
}
