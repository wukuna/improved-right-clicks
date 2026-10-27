package com.improvedmenus.rs2f;
import java.awt.Color;
import com.improvedmenus.rs2f.model.*;
import lombok.Builder;
import lombok.Getter;
@Builder @Getter public final class DisplayConfig {
    private final Color textColor;
    private final Color backgroundColor;
    private final Color borderColor;
    private final Boolean hidden;
    private final Boolean showLootbeam;
    private final Boolean showValue;
    private final Boolean compact;
    private final Boolean showDespawn;
    private final Boolean notify;
    private final TextAccent textAccent;
    private final Color textAccentColor;
    private final Color lootbeamColor;
    private final FontType fontType;
    private final Color menuTextColor;
    private final Boolean hideOverlay;

    private final Boolean highlightTile;
    private final Color tileStrokeColor;
    private final Color tileFillColor;

    private final SoundProvider sound;
    private final Integer menuSort;
    private final BufferedImageProvider icon;

}
