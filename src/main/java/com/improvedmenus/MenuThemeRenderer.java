package com.improvedmenus;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.Rasterizer;
import net.runelite.api.events.BeforeMenuRender;
import net.runelite.client.eventbus.Subscribe;

@Singleton
final class MenuThemeRenderer
{
    static final int BACKGROUND=0x372c1c, EDGE=0x14100a, LIGHT=0x9b8251, SHADE=0x5a4529;
    private final Client client;
    private final ImprovedMenusConfig config;

    @Inject MenuThemeRenderer(Client client, ImprovedMenusConfig config)
    {
        this.client=client;
        this.config=config;
    }

    @Subscribe(priority=-100)
    public void onBeforeMenuRender(BeforeMenuRender event)
    {
        if (event.isConsumed() || config.menuTheme()!=ImprovedMenusConfig.MenuTheme.OSRS || !client.isMenuOpen()) return;
        Menu menu=client.getMenu();
        if (menu==null || menu.getMenuWidth()<6 || menu.getMenuHeight()<22) return;

        for (MenuEntry entry:menu.getMenuEntries()) if (entry.getSubMenu()!=null) return;
        Rasterizer rasterizer=client.getRasterizer();
        if (rasterizer==null) return;
        surface(rasterizer,menu.getMenuX(),menu.getMenuY(),menu.getMenuWidth(),menu.getMenuHeight());

        client.drawOriginalMenu(0);
        event.consume();
    }

    static void surface(Rasterizer r,int x,int y,int width,int height)
    {
        r.fillRectangle(x,y,width,height,EDGE);
        r.fillRectangle(x+1,y+1,width-2,height-2,LIGHT);
        r.fillRectangle(x+1,y+height-2,width-2,1,SHADE);
        r.fillRectangle(x+width-2,y+1,1,height-2,SHADE);
        r.fillRectangle(x+2,y+2,width-4,height-4,BACKGROUND);

        r.fillRectangle(x+2,y+2,width-4,15,EDGE);
        r.fillRectangle(x+2,y+17,width-4,1,SHADE);
    }
}
