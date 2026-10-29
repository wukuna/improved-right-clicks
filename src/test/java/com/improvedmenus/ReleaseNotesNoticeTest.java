package com.improvedmenus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class ReleaseNotesNoticeTest
{
    @Test public void oncePerProfileAndDisabledCallbackCannotConsumeNotice()
    {
        Client client = mock(Client.class); ClientThread thread = mock(ClientThread.class);
        ConfigManager config = mock(ConfigManager.class); ChatMessageManager chat = mock(ChatMessageManager.class);
        List<Runnable> callbacks = new ArrayList<>();
        doAnswer(i -> { callbacks.add(i.getArgument(0)); return null; }).when(thread).invokeLater(any(Runnable.class));
        AtomicReference<String> receipt = new AtomicReference<>();
        when(config.getConfiguration(ImprovedMenusConfig.GROUP, ReleaseNotesNotice.KEY)).thenAnswer(i -> receipt.get());
        doAnswer(i -> { receipt.set(i.getArgument(2)); return null; }).when(config)
            .setConfiguration(ImprovedMenusConfig.GROUP, ReleaseNotesNotice.KEY, ReleaseNotesNotice.RELEASE);
        ReleaseNotesNotice notice = new ReleaseNotesNotice(client, thread, config, chat);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        notice.startUp(); notice.shutDown(); notice.startUp();
        callbacks.get(0).run(); verifyNoInteractions(chat, config);
        callbacks.get(1).run(); notice.showIfLoggedIn(); verify(chat, times(1)).queue(any());
        notice.shutDown(); notice.startUp(); callbacks.get(2).run(); verify(chat, times(1)).queue(any());
        receipt.set(null); notice.showIfLoggedIn(); verify(chat, times(2)).queue(any());
    }
}
