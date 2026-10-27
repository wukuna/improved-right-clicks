package com.improvedmenus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.components.TooltipComponent;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

/** Right-align only our row, leaving the shared tooltip stack owned by RuneLite. */
final class OptionCountTooltip extends TooltipComponent
{
    private final String text;
    private final TooltipManager manager;
    private final Client client;
    private Point location=new Point();

    OptionCountTooltip(String text,TooltipManager manager,Client client,Color background)
    {
        this.text=text; this.manager=manager; this.client=client;
        setText(text);
        if (background!=null) { setBackgroundColor(background); }
    }

    @Override public void setPreferredLocation(Point point) { location=new Point(point); }

    @Override public Dimension render(Graphics2D graphics)
    {
        // Measure using RuneLite's own text/tag sizing, without painting or rendering
        // another plugin's component (which could have side effects).
        Graphics2D measuring=(Graphics2D)graphics.create();
        Dimension own;
        int width;
        try
        {
            measuring.setClip(0,0,0,0);
            TooltipComponent measure=new TooltipComponent();
            measure.setText(text); own=measure.render(measuring); width=own.width;
            for (Tooltip tooltip : manager.getTooltips())
            {
                if (tooltip.getText()!=null)
                {
                    measure.setText(tooltip.getText()); width=Math.max(width,measure.render(measuring).width);
                }
                else if (tooltip.getComponent()!=null && tooltip.getComponent()!=this)
                {
                    Rectangle bounds=tooltip.getComponent().getBounds();
                    if (bounds!=null) { width=Math.max(width,bounds.width); }
                }
            }
        }
        finally { measuring.dispose(); }
        int x=Math.max(0,Math.min(location.x+width-own.width,client.getCanvasWidth()-own.width));
        int y=Math.max(0,Math.min(location.y,client.getCanvasHeight()-own.height));
        super.setPreferredLocation(new Point(x,y));
        super.render(graphics);
        return new Dimension(width,own.height);
    }
}
