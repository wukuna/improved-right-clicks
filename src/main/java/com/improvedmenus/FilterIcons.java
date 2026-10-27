package com.improvedmenus;
import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import com.improvedmenus.rs2f.LootFilter;
import com.improvedmenus.rs2f.model.*;
@Singleton
class FilterIcons {
 static final String KEY="lootIconFilter",BACKUP="lootIconFilterBackup";
 @Inject java.util.concurrent.ScheduledExecutorService executor;
 @Inject ConfigManager configs; @Inject Gson gson; @Inject Client client; @Inject ItemManager items;
 static final class Bundle { int version=1; String compressedSource; Map<String,String> images=new LinkedHashMap<>(); }
 static final class Prepared { Bundle bundle; LootFilter program; Map<String,BufferedImage> images=new LinkedHashMap<>(); }
 private volatile Prepared current;
 private String loaded;
 private volatile String loadStatus="No imported filter active";
 static String compress(String source) {
  try { ByteArrayOutputStream out=new ByteArrayOutputStream(); try(GZIPOutputStream gzip=new GZIPOutputStream(out)) { gzip.write(source.getBytes(StandardCharsets.UTF_8)); } return Base64.getEncoder().encodeToString(out.toByteArray()); }
  catch(IOException e) { throw new IllegalArgumentException("Cannot package filter",e); }
 }
 static String source(Bundle bundle) {
  if(bundle==null || bundle.version!=1 || bundle.compressedSource==null || bundle.compressedSource.length()>1500000) throw new IllegalArgumentException("Invalid filter backup");
  try(GZIPInputStream in=new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(bundle.compressedSource)))) {
   byte[] data=in.readNBytes(1000001); if(data.length>1000000) throw new IllegalArgumentException("Filter exceeds 1 MB"); return new String(data,StandardCharsets.UTF_8);
  } catch(IOException e) { throw new IllegalArgumentException("Invalid filter archive",e); }
 }
 Prepared prepare(Bundle bundle) {
  Prepared p=new Prepared(); p.bundle=bundle; p.program=LootFilter.compile(source(bundle));
  if(bundle.images==null || bundle.images.size()>128) throw new IllegalArgumentException("Use at most 128 images");
  for(String file:p.program.files()) {
   IconImages.safeName(file); String encoded=bundle.images.get(file);
   if(encoded==null) throw new IllegalArgumentException("Missing image: "+file);
   p.images.put(file,IconImages.decode(encoded));
  }
  return p;
 }
 Prepared parse(String json) {
  if(json.length()>3000000) throw new IllegalArgumentException("Filter backup exceeds 3 MB");
  try { return prepare(gson.fromJson(json,Bundle.class)); } catch(com.google.gson.JsonParseException e) { throw new IllegalArgumentException("Invalid filter backup",e); }
 }
 synchronized void reload() {
  String raw=configs.getConfiguration(ImprovedMenusConfig.GROUP,KEY);
  if(Objects.equals(raw,loaded)) return;
  loaded=raw; current=null;
  if(raw==null || raw.isBlank()) { loadStatus="No imported filter active"; return; }
  loadStatus="Loading imported filter...";
  executor.execute(() -> {
   Prepared next=null; String status="No imported filter active";
   try { next=parse(raw); } catch(IllegalArgumentException e) { status="Imported filter could not load; re-import its backup."; }
   synchronized(FilterIcons.this) { if(Objects.equals(raw,loaded)) { current=next; loadStatus=status; } }
  });
 }
 synchronized void save(Prepared p) {
  String before=configs.getConfiguration(ImprovedMenusConfig.GROUP,KEY);
  configs.setConfiguration(ImprovedMenusConfig.GROUP,BACKUP,before==null ? "" : before);
  String raw=p==null ? "" : gson.toJson(p.bundle);
  current=p; loaded=raw; loadStatus="No imported filter active"; configs.setConfiguration(ImprovedMenusConfig.GROUP,KEY,raw);
 }
 void undo() { String raw=configs.getConfiguration(ImprovedMenusConfig.GROUP,BACKUP); if(raw!=null) save(raw.isBlank() ? null : parse(raw)); }
 String export() { String raw=configs.getConfiguration(ImprovedMenusConfig.GROUP,KEY); return raw==null ? "" : raw; }
 String status() { Prepared p=current; return p==null ? loadStatus : p.program.rules.size()+" imported rules; "+p.images.size()+" embedded images"; }
 ItemIcons.Assignment find(MenuEntry entry) {
  Prepared p=current; if(p==null) return null;
  WorldView view=client.getWorldView(entry.getWorldViewId()); if(view==null) return null;
  int x=entry.getParam0(),y=entry.getParam1(),plane=view.getPlane(); Tile[][][] tiles=view.getScene().getTiles();
  if(plane<0 || plane>=tiles.length || x<0 || x>=tiles[plane].length || y<0 || y>=tiles[plane][x].length) return null;
  Tile tile=tiles[plane][x][y]; if(tile==null || tile.getGroundItems()==null) return null;
  BufferedImageProvider chosen=null; boolean found=false;
  for(TileItem item:tile.getGroundItems()) {
   if(item.getId()!=entry.getIdentifier()) continue;
   ItemComposition c=items.getItemComposition(item.getId());
   ItemComposition note=c.getLinkedNoteId()<0 ? null : items.getItemComposition(c.getLinkedNoteId());
   long price=item.getId()==995 ? 1 : item.getId()==13204 ? 1000 : items.getItemPrice(item.getId());
   ItemFacts facts=ItemFacts.builder().id(item.getId()).name(c.getName()).quantity(item.getQuantity()).ownership(item.getOwnership())
    .accountType(client.getVarbitValue(VarbitID.IRONMAN)).gePrice(price).haPrice(c.getHaPrice()).stackable(c.isStackable()).noted(c.getNote()!=-1)
    .tradeable(c.isTradeable() || note!=null && note.isTradeable()).geTradeable(c.isGeTradeable() || note!=null && note.isGeTradeable())
    .worldPoint(WorldPoint.fromLocalInstance(client,tile.getLocalLocation())).build();
   BufferedImageProvider icon=p.program.find(facts);
   if(found && !Objects.equals(chosen,icon)) return null; // Menu cannot distinguish ambiguous stacks.
   chosen=icon; found=true;
  }
  if(chosen==null) return null;
  ItemIcons.Assignment a=new ItemIcons.Assignment(); a.itemId=entry.getIdentifier();
  if(chosen instanceof BufferedImageProvider.CurrentItem) a.kind=ItemIcons.Kind.CURRENT_ITEM;
  else if(chosen instanceof BufferedImageProvider.Item) { a.kind=ItemIcons.Kind.ITEM; a.iconId=((BufferedImageProvider.Item)chosen).getItemId(); if(a.iconId>=client.getItemCount()) return null; }
  else if(chosen instanceof BufferedImageProvider.Sprite) { a.kind=ItemIcons.Kind.SPRITE; a.iconId=((BufferedImageProvider.Sprite)chosen).getSpriteId(); if(a.iconId==9000000) return null; a.spriteIndex=((BufferedImageProvider.Sprite)chosen).getIndex(); }
  else { a.kind=ItemIcons.Kind.IMAGE; a.image=p.bundle.images.get(((BufferedImageProvider.File)chosen).getFilename()); }
  return a;
 }
}
