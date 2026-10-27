package com.improvedmenus;

import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class RenameNoticeTest
{
    @Test public void waitsForLoginAndDoesNotRepeatAfterRestart()
    {
        Client client = mock(Client.class);
        ClientThread thread = mock(ClientThread.class);
        ConfigManager config = mock(ConfigManager.class);
        ChatMessageManager chat = mock(ChatMessageManager.class);
        AtomicReference<String> seen = new AtomicReference<>();
        when(config.getConfiguration(ImprovedMenusConfig.GROUP, RenameNotice.KEY)).thenAnswer(i -> seen.get());
        doAnswer(i -> { seen.set(i.getArgument(2)); return null; }).when(config).setConfiguration(ImprovedMenusConfig.GROUP, RenameNotice.KEY, "true");
        RenameNotice notice = new RenameNotice(client, thread, config, chat);
        notice.startUp();
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        notice.showIfLoggedIn();
        verifyNoInteractions(chat);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        notice.showIfLoggedIn();
        notice.showIfLoggedIn();
        notice.shutDown();
        RenameNotice restarted = new RenameNotice(client, thread, config, chat);
        restarted.startUp();
        restarted.showIfLoggedIn();
        verify(chat, times(1)).queue(any(QueuedMessage.class));
        verify(config, times(1)).setConfiguration(ImprovedMenusConfig.GROUP, RenameNotice.KEY, "true");
    }

    @Test public void disabledBeforeDeferredCallbackDoesNotNotify()
    {
        Client client = mock(Client.class);
        ChatMessageManager chat = mock(ChatMessageManager.class);
        ConfigManager config = mock(ConfigManager.class);
        RenameNotice notice = new RenameNotice(client, mock(ClientThread.class), config, chat);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        notice.startUp();
        notice.shutDown();
        notice.showIfLoggedIn();
        verifyNoInteractions(chat, config);
    }
}
