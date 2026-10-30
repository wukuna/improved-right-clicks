package com.improvedmenus;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;

@Singleton
final class ReleaseNotesNotice
{
    static final String RELEASE = "2026-10-30-improved-looting-provider";
    static final String KEY = "recentChangesSeen";
    private final Client client;
    private final ClientThread thread;
    private final ConfigManager config;
    private final ChatMessageManager chat;
    private volatile boolean active;
    private volatile int generation;

    @Inject ReleaseNotesNotice(Client client, ClientThread thread, ConfigManager config, ChatMessageManager chat)
    { this.client = client; this.thread = thread; this.config = config; this.chat = chat; }

    void startUp()
    {
        active = true;
        int request = ++generation;
        thread.invokeLater(() -> { if (request == generation) showIfLoggedIn(); });
    }
    void shutDown() { active = false; generation++; }
    void showIfLoggedIn()
    {
        if (!active || client.getGameState() != GameState.LOGGED_IN
            || RELEASE.equals(config.getConfiguration(ImprovedMenusConfig.GROUP, KEY))) return;
        chat.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).value(
            "Improved Right-Clicks: Choose the optional OSRS menu theme in Presentation for brown panels and bronze borders matching the loot cards. Native remains the default; nested menus and other menu renderers keep their appearance. Improved Looting can supply resolved menu icons, optional colors and priority; enable its Connect menu icons switch and choose the separate loot-display options here. Loot Filters remains a fallback with a compatible provider. Player badges remain individually toggleable.").build());
        config.setConfiguration(ImprovedMenusConfig.GROUP, KEY, RELEASE);
    }
}
