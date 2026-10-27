package com.improvedmenus;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Queue after dynamic tooltip producers, immediately before the tooltip renderer. */
@Singleton
public final class OptionCountOverlay extends Overlay
{
    @Inject private MenuPresentation presentation;

    public OptionCountOverlay()
    {
        setPosition(OverlayPosition.TOOLTIP);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGHEST + 1);
        drawAfterInterface(InterfaceID.TOPLEVEL_DISPLAY);
    }

    @Override public Dimension render(Graphics2D graphics)
    {
        presentation.queueOptionCount();
        return null;
    }
}
