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
    static final String RELEASE = "2026-11-02-ironman-identification";
    static final String KEY = "recentChangesSeen";
    private static final String[] UPDATES = {
        "Added one optional Ironman identification (Beta) setting.",
        "Identifies nearby players using chat and limited Hiscores checks.",
        "Remembers results across hops and restarts, with timed expiry.",
    };
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
            "[Improved Right-Clicks] Updates:<br>* " + String.join("<br>* ", UPDATES)).build());
        config.setConfiguration(ImprovedMenusConfig.GROUP, KEY, RELEASE);
    }
}
