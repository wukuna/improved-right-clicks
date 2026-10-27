package com.improvedmenus;
import com.google.inject.*;
import java.lang.reflect.*;
import java.util.*;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.*;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
public class UserCombinationAuditTest {
 @Test public void booleanPairsAndToggleTransitions() throws Exception {
  Map<String,Boolean> flags=new HashMap<>();
  ImprovedMenusConfig config=mock(ImprovedMenusConfig.class,i -> flags.containsKey(i.getMethod().getName()) ? flags.get(i.getMethod().getName()) : i.callRealMethod());
  when(config.revealKey()).thenReturn(ImprovedMenusConfig.RevealKey.NONE);
  when(config.itemRules()).thenReturn("[inventory]Drop,*"); when(config.colors()).thenReturn("*:*:#123abc");
  Client client=mock(Client.class); Widget bank=mock(Widget.class); boolean[] bankOpen={false};
  when(bank.isHidden()).thenAnswer(i -> !bankOpen[0]); when(client.getWidget(InterfaceID.BANKMAIN,0)).thenReturn(bank);
  when(client.getMouseCanvasPosition()).thenReturn(new Point(110,231));
  MenuEngine engine=Guice.createInjector(new AbstractModule(){protected void configure(){bind(net.runelite.client.config.RuneLiteConfig.class).toInstance(mock(net.runelite.client.config.RuneLiteConfig.class));bind(Client.class).toInstance(client);bind(ImprovedMenusConfig.class).toInstance(config);bind(ItemManager.class).toInstance(mock(ItemManager.class));}}).getInstance(MenuEngine.class);
  List<String> keys=new ArrayList<>(); for(Method m:ImprovedMenusConfig.class.getDeclaredMethods()) if(m.getReturnType()==boolean.class)keys.add(m.getName()); Collections.sort(keys);
  List<Map<String,Boolean>> profiles=new ArrayList<>();
  for(boolean background:new boolean[]{false,true}){
   Map<String,Boolean> base=new HashMap<>();for(String k:keys)base.put(k,background);profiles.add(base);
   for(int a=0;a<keys.size();a++)for(int b=a+1;b<keys.size();b++)for(int bits=0;bits<4;bits++){
    Map<String,Boolean> f=new HashMap<>(base);f.put(keys.get(a),(bits&1)!=0);f.put(keys.get(b),(bits&2)!=0);profiles.add(f);
   }
  }
  int cases=0;
  MenuEntry[] initial={e("Cancel","",MenuAction.CANCEL,-1,0),e("Walk here","",MenuAction.WALK,-1,0),e("Set heading","",MenuAction.SET_HEADING,-1,0),e("Empty","Prayer potion(4)",MenuAction.CC_OP,100,InterfaceID.INVENTORY),e("Drink","Prayer potion(4)",MenuAction.CC_OP,100,InterfaceID.INVENTORY),e("Eat","Shark",MenuAction.CC_OP,101,InterfaceID.INVENTORY),e("Wield","Rune sword",MenuAction.CC_OP,102,InterfaceID.INVENTORY),e("Wear","Rune platebody",MenuAction.CC_OP,103,InterfaceID.INVENTORY),e("Examine","Rune sword",MenuAction.CC_OP_LOW_PRIORITY,102,InterfaceID.INVENTORY),e("Take","Coins",MenuAction.GROUND_ITEM_THIRD_OPTION,104,0),e("Examine","Coins",MenuAction.EXAMINE_ITEM_GROUND,104,0),e("Talk-to","Goblin",MenuAction.NPC_FIRST_OPTION,-1,0),e("Examine","Goblin",MenuAction.EXAMINE_NPC,-1,0),e("Open","Door",MenuAction.GAME_OBJECT_FIRST_OPTION,-1,0),e("Examine","Door",MenuAction.EXAMINE_OBJECT,-1,0),e("Drop","Rune sword",MenuAction.CC_OP,102,InterfaceID.INVENTORY)};
  String[] original=Arrays.stream(initial).map(MenuEntry::getOption).toArray(String[]::new);
  Menu menu=menu(initial); when(menu.getMenuX()).thenReturn(100);when(menu.getMenuY()).thenReturn(200);when(menu.getMenuWidth()).thenReturn(300);when(menu.getMenuHeight()).thenReturn(300);
  for(Map<String,Boolean> profile:profiles)for(boolean inBank:new boolean[]{false,true}){
   flags.clear();flags.putAll(profile);bankOpen[0]=inBank;engine.reload();menu.setMenuEntries(initial);for(int n=0;n<initial.length;n++)initial[n].setOption(original[n]);
   engine.applyOpened(menu);Set<String> ops=new HashSet<>();for(MenuEntry e:menu.getMenuEntries())ops.add(MenuRules.plain(e.getOption()));
   String label=profile+" bank="+inBank;
   assertEquals(label,!flags.get("hideCancel"),ops.contains("Cancel"));assertEquals(label,!flags.get("hideWalkHere"),ops.contains("Walk here"));assertEquals(label,!flags.get("hideSetHeading"),ops.contains("Set heading"));
   assertEquals(label,!flags.get("potionEmpty"),ops.contains("Empty"));
   for(String op:new String[]{"Drink","Eat","Wield","Wear"}) assertEquals(label,!(inBank&&flags.get("bank"+op)),ops.contains(op));
   assertFalse(label,ops.contains("Drop"));assertTrue(label,ops.contains("Take"));assertTrue(label,ops.contains("Talk-to"));assertTrue(label,ops.contains("Open"));
   assertEquals(label,!flags.get("examineInventory"),contains(menu,"Examine","Rune sword"));assertEquals(label,!flags.get("examineGround"),contains(menu,"Examine","Coins"));assertEquals(label,!flags.get("examineNpc"),contains(menu,"Examine","Goblin"));assertEquals(label,!flags.get("examineObject"),contains(menu,"Examine","Door"));
   engine.highlightHovered(menu);engine.clear();for(int n=0;n<initial.length;n++)assertEquals(label,original[n],initial[n].getOption());cases++;
  }
  System.out.println("AUDIT menu boolean keys="+keys.size()+" profiles="+profiles.size()+" bank contexts="+cases);
 }
 static boolean contains(Menu m,String o,String t){for(MenuEntry e:m.getMenuEntries())if(MenuRules.plain(e.getOption()).equals(o)&&e.getTarget().equals(t))return true;return false;}
 static MenuEntry e(String option,String target,MenuAction type,int item,int group){
  Map<String,Object> p=new HashMap<>();p.put("Option",option);p.put("Target",target);p.put("Type",type);p.put("ItemId",item);p.put("Param1",group<<16);p.put("Identifier",item);
  return (MenuEntry)Proxy.newProxyInstance(MenuEntry.class.getClassLoader(),new Class[]{MenuEntry.class},(obj,m,a)->{
   if(m.getName().startsWith("get"))return p.getOrDefault(m.getName().substring(3),def(m.getReturnType()));
   if(m.getName().startsWith("set")){p.put(m.getName().substring(3),a[0]);return m.getReturnType()==MenuEntry.class?obj:null;}
   if(m.getName().equals("hashCode"))return System.identityHashCode(obj);if(m.getName().equals("equals"))return obj==a[0];return def(m.getReturnType());});
 }
 static Object def(Class<?> c){if(c==boolean.class)return false;if(c==int.class)return 0;return null;}
 static Menu menu(MenuEntry... es){Menu m=mock(Menu.class);MenuEntry[][] p={es};when(m.getMenuEntries()).thenAnswer(i->p[0]);doAnswer(i->{p[0]=i.getArgument(0);return null;}).when(m).setMenuEntries(any(MenuEntry[].class));return m;}
 @Test public void groundIconRedecorationMustRemainRestorable(){
  Client client=mock(Client.class);ImprovedMenusConfig config=mock(ImprovedMenusConfig.class);when(config.groundIcons()).thenReturn(true);
  ChatIconManager icons=mock(ChatIconManager.class);when(icons.registerChatIcon(any())).thenReturn(2);when(icons.chatIconIndex(2)).thenReturn(4);
  MenuPresentation p=Guice.createInjector(new AbstractModule(){protected void configure(){bind(net.runelite.client.config.RuneLiteConfig.class).toInstance(mock(net.runelite.client.config.RuneLiteConfig.class));bind(Client.class).toInstance(client);bind(ImprovedMenusConfig.class).toInstance(config);bind(ChatIconManager.class).toInstance(icons);bind(ItemManager.class).toInstance(mock(ItemManager.class));bind(TooltipManager.class).toInstance(mock(TooltipManager.class));}}).getInstance(MenuPresentation.class);
  p.start();MenuEntry take=e("Take","Coins",MenuAction.GROUND_ITEM_THIRD_OPTION,1,0);Menu menu=menu(take);p.decorate(menu);p.decorate(menu);p.stop();assertEquals("Repeated opening/decorating then disabling must restore the item name","Coins",take.getTarget());
 }
 @Test public void closedSubmenuMustNotDrawGroundIcons() {
  Client client=mock(Client.class);ImprovedMenusConfig config=mock(ImprovedMenusConfig.class);when(config.groundIcons()).thenReturn(true);when(client.isMenuOpen()).thenReturn(true);
  ChatIconManager icons=mock(ChatIconManager.class);when(icons.registerChatIcon(any())).thenReturn(2);when(icons.chatIconIndex(2)).thenReturn(4);ItemManager items=mock(ItemManager.class);
  MenuPresentation p=Guice.createInjector(new AbstractModule(){protected void configure(){bind(net.runelite.client.config.RuneLiteConfig.class).toInstance(mock(net.runelite.client.config.RuneLiteConfig.class));bind(Client.class).toInstance(client);bind(ImprovedMenusConfig.class).toInstance(config);bind(ChatIconManager.class).toInstance(icons);bind(ItemManager.class).toInstance(items);bind(TooltipManager.class).toInstance(mock(TooltipManager.class));}}).getInstance(MenuPresentation.class);
  MenuEntry take=e("Take","Coins",MenuAction.GROUND_ITEM_THIRD_OPTION,100,0);Menu sub=menu(take);when(sub.getMenuWidth()).thenReturn(200);when(sub.getMenuHeight()).thenReturn(60);
  MenuEntry parent=mock(MenuEntry.class,RETURNS_SELF);when(parent.getOption()).thenReturn("More");when(parent.getTarget()).thenReturn("");when(parent.getType()).thenReturn(MenuAction.RUNELITE);when(parent.getSubMenu()).thenReturn(sub);
  Menu root=menu(parent);when(root.getMenuWidth()).thenReturn(200);when(root.getMenuHeight()).thenReturn(60);when(client.getMenu()).thenReturn(root);p.start();p.decorate(root);
  java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(500,300,java.awt.image.BufferedImage.TYPE_INT_ARGB);java.awt.Graphics2D g=image.createGraphics();try{p.render(g);}finally{g.dispose();}
  verify(items,never()).getImage(anyInt(),anyInt(),anyBoolean());
 }
 @Test public void iconRestorePreservesLaterTargetsAndRemappedEntries(){
  Client client=mock(Client.class);ImprovedMenusConfig config=mock(ImprovedMenusConfig.class);when(config.groundIcons()).thenReturn(true);ChatIconManager icons=mock(ChatIconManager.class);when(icons.registerChatIcon(any())).thenReturn(2);when(icons.chatIconIndex(2)).thenReturn(4);
  MenuPresentation p=Guice.createInjector(new AbstractModule(){protected void configure(){bind(net.runelite.client.config.RuneLiteConfig.class).toInstance(mock(net.runelite.client.config.RuneLiteConfig.class));bind(Client.class).toInstance(client);bind(ImprovedMenusConfig.class).toInstance(config);bind(ChatIconManager.class).toInstance(icons);bind(ItemManager.class).toInstance(mock(ItemManager.class));bind(TooltipManager.class).toInstance(mock(TooltipManager.class));}}).getInstance(MenuPresentation.class);p.start();
  MenuEntry entry=e("Take","Coins",MenuAction.GROUND_ITEM_THIRD_OPTION,1,0);p.decorate(menu(entry));entry.setTarget("<col=ff0000>Coins</col>");p.stop();assertEquals("<col=ff0000>Coins</col>",entry.getTarget());
  entry=e("Take","Coins",MenuAction.GROUND_ITEM_THIRD_OPTION,1,0);p.decorate(menu(entry));entry.setIdentifier(2);p.stop();assertEquals("<img=4>Coins",entry.getTarget());
  MenuEntry root=e("Take","Coins",MenuAction.GROUND_ITEM_THIRD_OPTION,1,0);p.decorate(menu(root));when(config.groundIcons()).thenReturn(false);p.decorate(menu(root));assertEquals("Coins",root.getTarget());
 }
}
