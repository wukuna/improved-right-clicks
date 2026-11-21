package com.improvedmenus;

import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.*;
import static org.mockito.Mockito.*;

final class TestMenus
{
	static MenuEntry entry(String option, String target, MenuAction type, int item, int group)
	{
		MenuEntry e = mock(MenuEntry.class, RETURNS_SELF);
		AtomicReference<String> op = new AtomicReference<>(option), name = new AtomicReference<>(target);
		when(e.getOption()).thenAnswer(i -> op.get());
		when(e.getTarget()).thenAnswer(i -> name.get());
		when(e.setOption(anyString())).thenAnswer(i ->
		{
			op.set(i.getArgument(0));
			return e;
		});
		when(e.setTarget(anyString())).thenAnswer(i ->
		{
			name.set(i.getArgument(0));
			return e;
		});
		when(e.getType()).thenReturn(type);
		when(e.getItemId()).thenReturn(item);
		when(e.getParam1()).thenReturn(group << 16);
		return e;
	}

	static Menu menu(MenuEntry... entries)
	{
		Menu m = mock(Menu.class);
		AtomicReference<MenuEntry[]> state = new AtomicReference<>(entries);
		when(m.getMenuEntries()).thenAnswer(i -> state.get());
		doAnswer(i ->
		{
			state.set(i.getArgument(0));
			return null;
		}).when(m).setMenuEntries(any(MenuEntry[].class));
		return m;
	}

	private TestMenus()
	{
	}
}
