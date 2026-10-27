package com.improvedmenus;

import com.google.inject.*;
import java.awt.Canvas;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.input.MouseManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.callback.ClientThread;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ButtonGuardTest
{
    private Client client; private ImprovedMenusConfig config; private MouseManager mouse; private ButtonGuard guard;
    private final AtomicReference<MouseListener> listener=new AtomicReference<>();
    @Before public void setup()
    {
        client=mock(Client.class); config=mock(ImprovedMenusConfig.class); mouse=mock(MouseManager.class);
        when(config.revealKey()).thenReturn(ImprovedMenusConfig.RevealKey.NONE);
        when(config.autoRetaliate()).thenReturn(ClickMode.NORMAL); when(config.xpDrops()).thenReturn(ClickMode.NORMAL);
        when(config.alwaysSetPlaceholders()).thenReturn(ClickMode.NORMAL); when(config.report()).thenReturn(ClickMode.NORMAL);
        doAnswer(i -> { listener.set(i.getArgument(0)); return null; }).when(mouse).registerMouseListener(any(MouseListener.class));
        ClientThread thread=mock(ClientThread.class);
        doAnswer(i -> { ((Runnable)i.getArgument(0)).run(); return null; }).when(thread).invoke(any(Runnable.class));
        doAnswer(i -> { ((Runnable)i.getArgument(0)).run(); return null; }).when(thread).invokeLater(any(Runnable.class));
        guard=Guice.createInjector(new AbstractModule() { protected void configure() {
            bind(Client.class).toInstance(client); bind(ImprovedMenusConfig.class).toInstance(config); bind(MouseManager.class).toInstance(mouse);
            bind(ClientThread.class).toInstance(thread);
        } }).getInstance(ButtonGuard.class);
        guard.startUp();
    }
    private MenuEntry button(String option,int group)
    {
        MenuEntry e=TestMenus.entry(option,"",MenuAction.CC_OP,-1,group); Widget widget=mock(Widget.class); when(widget.getId()).thenReturn(group<<16);
        when(e.getWidget()).thenReturn(widget); return e;
    }
    private MenuOptionClicked click(MenuEntry e)
    { MenuOptionClicked event=mock(MenuOptionClicked.class); when(event.getMenuEntry()).thenReturn(e); guard.onMenuOptionClicked(event); return event; }
    @Test public void normalButtonsPassThrough() { verify(click(button("Auto Retaliate",InterfaceID.COMBAT_INTERFACE)),never()).consume(); }
    @Test public void disabledButtonsConsumeEvenFromOpenMenu()
    {
        when(config.autoRetaliate()).thenReturn(ClickMode.DISABLED); press(true); verify(click(button("Auto Retaliate",InterfaceID.COMBAT_INTERFACE))).consume();
    }
    private void press(boolean open)
    {
        when(client.isMenuOpen()).thenReturn(open);
        listener.get().mousePressed(new MouseEvent(new Canvas(),MouseEvent.MOUSE_PRESSED,1,0,1,1,1,false,MouseEvent.BUTTON1));
    }
    @Test public void rightClickOnlyBlocksDirectLeftClick()
    { when(config.autoRetaliate()).thenReturn(ClickMode.LEFT_CLICK_OFF); press(false); verify(click(button("Auto Retaliate",InterfaceID.COMBAT_INTERFACE))).consume(); }
    @Test public void rightClickOnlyAllowsSelectionFromOpenMenu()
    { when(config.autoRetaliate()).thenReturn(ClickMode.LEFT_CLICK_OFF); press(true); verify(click(button("Auto Retaliate",InterfaceID.COMBAT_INTERFACE)),never()).consume(); }
    @Test public void selectionOriginIsConsumedOnce()
    {
        when(config.autoRetaliate()).thenReturn(ClickMode.LEFT_CLICK_OFF); press(true);
        click(button("Auto Retaliate",InterfaceID.COMBAT_INTERFACE)); verify(click(button("Auto Retaliate",InterfaceID.COMBAT_INTERFACE))).consume();
    }
    @Test public void resetClearsStaleMouseOrigin()
    { when(config.autoRetaliate()).thenReturn(ClickMode.LEFT_CLICK_OFF); press(true); guard.reset(); verify(click(button("Auto Retaliate",InterfaceID.COMBAT_INTERFACE))).consume(); }
    @Test public void labelsCannotProtectUnrelatedWidget()
    { when(config.autoRetaliate()).thenReturn(ClickMode.DISABLED); assertNull(GuardedAction.find(button("Auto Retaliate",InterfaceID.INVENTORY))); }
    @Test public void allFourGameframeModesMatchXpControls()
    {
        for (int group : new int[]{InterfaceID.ORBS,InterfaceID.ORBS_NOMAP,InterfaceID.ORBS_OSM,InterfaceID.ORBS_OSM_NOMAP})
        { assertNotNull(GuardedAction.find(button("Show XP drops",group))); }
    }
    @Test public void bankAndBothReportActionsMatch()
    {
        assertNotNull(GuardedAction.find(button("Always set placeholders",InterfaceID.BANKMAIN)));
        assertNotNull(GuardedAction.find(button("Report abuse",InterfaceID.CHATBOX)));
        assertNotNull(GuardedAction.find(button("Report game bug",InterfaceID.CHATBOX)));
    }
    @Test public void disabledRemovedWithStableRetainedOrder()
    {
        when(config.report()).thenReturn(ClickMode.DISABLED); MenuEntry cancel=TestMenus.entry("Cancel","",MenuAction.CANCEL,-1,0), report=button("Report abuse",InterfaceID.CHATBOX), other=button("Settings",InterfaceID.CHATBOX);
        Menu m=TestMenus.menu(cancel,report,other); when(client.getMenu()).thenReturn(m); guard.onPostMenuSort(new PostMenuSort());
        assertArrayEquals(new MenuEntry[]{cancel,other},m.getMenuEntries());
    }
    @Test public void tooltipRestoresOwnedInstanceAndNeverTouchesReplacement()
    {
        when(config.autoRetaliate()).thenReturn(ClickMode.DISABLED); Widget original=mock(Widget.class), replacement=mock(Widget.class);
        Menu menu=TestMenus.menu(button("Auto Retaliate",InterfaceID.COMBAT_INTERFACE));
        when(client.getWidget(InterfaceID.CombatInterface.TOOLTIP)).thenReturn(original); when(client.getMenu()).thenReturn(menu);
        guard.onPostMenuSort(new PostMenuSort()); verify(original).setHidden(true);
        when(client.getWidget(InterfaceID.CombatInterface.TOOLTIP)).thenReturn(replacement); guard.reset(); verify(original).setHidden(false); verify(replacement,never()).setHidden(anyBoolean());
    }
    @Test public void alreadyHiddenTooltipRemainsHiddenOnShutdown()
    {
        when(config.autoRetaliate()).thenReturn(ClickMode.DISABLED); Widget tooltip=mock(Widget.class); when(tooltip.isHidden()).thenReturn(true);
        Menu menu=TestMenus.menu(button("Auto Retaliate",InterfaceID.COMBAT_INTERFACE));
        when(client.getWidget(InterfaceID.CombatInterface.TOOLTIP)).thenReturn(tooltip); when(client.getMenu()).thenReturn(menu);
        guard.onPostMenuSort(new PostMenuSort()); guard.shutDown(); verify(tooltip,never()).setHidden(false); verify(mouse).unregisterMouseListener(listener.get());
    }
    @Test public void nonWidgetActionsNeverMatch()
    {
        MenuEntry e=button("Auto Retaliate",InterfaceID.COMBAT_INTERFACE); when(e.getType()).thenReturn(MenuAction.NPC_FIRST_OPTION); assertNull(GuardedAction.find(e));
    }
    @Test public void auditAllButtonModesWithCleanupAndClickOrigins()
    {
        MenuEngine engine=Guice.createInjector(new AbstractModule() { protected void configure() {
            bind(Client.class).toInstance(client); bind(ImprovedMenusConfig.class).toInstance(config);
            bind(net.runelite.client.game.ItemManager.class).toInstance(mock(net.runelite.client.game.ItemManager.class));
        } }).getInstance(MenuEngine.class);
        when(config.itemRules()).thenReturn("");when(config.colors()).thenReturn("");when(config.exceptions()).thenReturn("");engine.reload();
        String[] options={"Auto Retaliate","Show XP drops","Always set placeholders","Report abuse"};
        int[] groups={InterfaceID.COMBAT_INTERFACE,InterfaceID.ORBS,InterfaceID.BANKMAIN,InterfaceID.CHATBOX};int cases=0;
        for(ClickMode mode:ClickMode.values())for(int settings=0;settings<8;settings++)for(int target=0;target<4;target++)for(boolean fromMenu:new boolean[]{false,true})
        {
            when(config.autoRetaliate()).thenReturn(mode);when(config.xpDrops()).thenReturn(mode);when(config.alwaysSetPlaceholders()).thenReturn(mode);when(config.report()).thenReturn(mode);
            when(config.hideCancel()).thenReturn((settings&1)!=0);when(config.hideWalkHere()).thenReturn((settings&2)!=0);when(config.deprioritizeLeftClickOffEntries()).thenReturn((settings&4)!=0);
            MenuEntry entry=button(options[target],groups[target]),cancel=TestMenus.entry("Cancel","",MenuAction.CANCEL,-1,0);Menu menu=TestMenus.menu(cancel,entry);when(client.getMenu()).thenReturn(menu);when(client.isMenuOpen()).thenReturn(false);
            guard.onPostMenuSort(new PostMenuSort());guard.onMenuOpened(new MenuOpened());engine.applyOpened(menu);assertTrue("Cleanup must retain a safe fallback",menu.getMenuEntries().length>0);
            press(fromMenu);MenuOptionClicked event=click(entry);if(mode.removesMenuEntry()||mode.blocksLeftClick()&&!fromMenu)verify(event).consume();else verify(event,never()).consume();guard.reset();cases++;
        }guard.shutDown();System.out.println("AUDIT button protection/cleanup/origin cases="+cases);
    }
}
