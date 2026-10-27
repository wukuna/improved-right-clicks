package com.improvedmenus;
import com.google.inject.*;
import net.runelite.api.*;
import net.runelite.client.game.ItemManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
public class UpstreamToolRegressionTest {
 private MenuEngine engine(){Client c=mock(Client.class);ImprovedMenusConfig cfg=mock(ImprovedMenusConfig.class,CALLS_REAL_METHODS);when(cfg.chop()).thenReturn(true);when(cfg.mine()).thenReturn(true);return Guice.createInjector(new AbstractModule(){protected void configure(){bind(Client.class).toInstance(c);bind(ImprovedMenusConfig.class).toInstance(cfg);bind(ItemManager.class).toInstance(mock(ItemManager.class));}}).getInstance(MenuEngine.class);}
 private String reason(MenuEngine e,String option,String target,int id){MenuEntry entry=TestMenus.entry(option,target,MenuAction.GAME_OBJECT_FIRST_OPTION,-1,0);when(entry.getIdentifier()).thenReturn(id);MenuEngine.Context context=new MenuEngine.Context();context.toolsKnown=true;return e.reason(entry,new MenuEntry[]{entry},target,option,context);}
 @Test public void knifeSwampTreeMustNotRequireAnAxe(){assertNull(reason(engine(),"Cut","Swamp tree",13847));}
 @Test public void questCaveEntryRemainsAvailableWithoutPickaxe(){assertNull(reason(engine(),"Mine","Cave entrance",0));}
 @Test public void ordinaryTreesAndRocksStayProtected(){MenuEngine e=engine();assertNotNull(reason(e,"Chop down","Tree",1));assertNotNull(reason(e,"Mine","Rocks",2));}
}
