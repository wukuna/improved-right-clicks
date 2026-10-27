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
final class RenameNotice
{
    static final String KEY = "rightClicksRenameNoticeShown";
    private final Client client;
    private final ClientThread clientThread;
    private final ConfigManager config;
    private final ChatMessageManager chat;
    private volatile boolean active;

    @Inject
    RenameNotice(Client client, ClientThread clientThread, ConfigManager config, ChatMessageManager chat)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.config = config;
        this.chat = chat;
    }

    void startUp()
    {
        active = true;
        clientThread.invokeLater(this::showIfLoggedIn);
    }

    void shutDown()
    {
        active = false;
    }

    void showIfLoggedIn()
    {
        if (!active || client.getGameState() != GameState.LOGGED_IN
            || Boolean.parseBoolean(config.getConfiguration(ImprovedMenusConfig.GROUP, KEY)))
        {
            return;
        }
        chat.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE)
            .value("Menu Workshop is now Improved Right-Clicks. Your saved settings and profiles are unchanged. Find us under the new name in plugin settings.")
            .build());
        config.setConfiguration(ImprovedMenusConfig.GROUP, KEY, "true");
    }
}
