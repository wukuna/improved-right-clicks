package com.improvedmenus;

import com.google.inject.Guice;
import com.google.gson.Gson;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.util.*;
import javax.swing.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.ConfigProfile;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MenuSidebarSettingsTest
{
	@Test
	public void editsAreSingleKeyAndStaleProfilesCannotChangeSettings() throws Exception
	{
		ConfigManager manager = mock(ConfigManager.class);
		ConfigProfile profile = mock(ConfigProfile.class);
		when(profile.getId()).thenReturn(1L);
		when(manager.getProfile()).thenReturn(profile);
		Map<String, String> values = new HashMap<>();
		values.put("itemRules", "Use,Item one\nDrop,Item two");
		when(manager.getConfiguration(eq(ImprovedMenusConfig.GROUP), anyString()))
				.thenAnswer(call -> values.get(call.getArgument(1)));
		doAnswer(call -> { values.put(call.getArgument(1), call.getArgument(2)); return null; })
				.when(manager).setConfiguration(eq(ImprovedMenusConfig.GROUP), anyString(), any());
		ProfileStore store = Guice.createInjector(binder ->
		{
			binder.bind(ConfigManager.class).toInstance(manager);
			binder.bind(Gson.class).toInstance(new Gson());
		}).getInstance(ProfileStore.class);
		SwingUtilities.invokeAndWait(() ->
		{
			MenuSidebarSettings settings = new MenuSidebarSettings(store, Runnable::run);
			JPanel page = settings.actions();
			settings.refresh();
			verify(manager, never()).setConfiguration(anyString(), anyString(), any());
			JTextArea rules = (JTextArea) find(page, "itemRules");
			assertEquals(values.get("itemRules"), rules.getText());
			for (var listener : rules.getFocusListeners())
				listener.focusLost(new FocusEvent(rules, FocusEvent.FOCUS_LOST));
			verify(manager, never()).setConfiguration(anyString(), anyString(), any());
			((JCheckBox) find(page, "chop")).doClick();
			verify(manager).setConfiguration("improvedmenus", "chop", "true");
			when(profile.getId()).thenReturn(2L);
			((JCheckBox) find(page, "mine")).doClick();
			verify(manager, never()).setConfiguration(eq("improvedmenus"), eq("mine"), any());
		});
	}

	private static Component find(Container parent, String key)
	{
		for (Component child : parent.getComponents())
		{
			if (key.equals(child.getName())) return child;
			if (child instanceof Container)
			{
				Component found = find((Container) child, key);
				if (found != null) return found;
			}
		}
		return null;
	}
}
