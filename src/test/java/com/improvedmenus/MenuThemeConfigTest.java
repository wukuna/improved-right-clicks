package com.improvedmenus;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MenuThemeConfigTest
{
	@Test
	public void customIconProviderDefaultsOffDespiteLegacyOptInAndRoundTripsEveryChoice() throws Exception
	{
		ConfigManager manager = mock(ConfigManager.class, CALLS_REAL_METHODS);
		doReturn(null).when(manager).getConfiguration(eq(ImprovedMenusConfig.GROUP), anyString());
		doReturn("true").when(manager).getConfiguration(ImprovedMenusConfig.GROUP, "lootFiltersCompatibility");
		Constructor<?> constructor = Class.forName("net.runelite.client.config.ConfigInvocationHandler")
				.getDeclaredConstructor(ConfigManager.class);
		constructor.setAccessible(true);
		ImprovedMenusConfig config = (ImprovedMenusConfig) Proxy.newProxyInstance(
				ImprovedMenusConfig.class.getClassLoader(), new Class<?>[] { ImprovedMenusConfig.class },
				(InvocationHandler) constructor.newInstance(manager));
		assertTrue(config.lootFiltersCompatibility());
		assertEquals(ImprovedMenusConfig.CustomIconProvider.OFF, config.customIconProvider());
		for (ImprovedMenusConfig.CustomIconProvider provider : ImprovedMenusConfig.CustomIconProvider.values())
		{
			doReturn(provider.name()).when(manager).getConfiguration(ImprovedMenusConfig.GROUP, "customIconProvider");
			config = (ImprovedMenusConfig) Proxy.newProxyInstance(ImprovedMenusConfig.class.getClassLoader(),
					new Class<?>[] { ImprovedMenusConfig.class }, (InvocationHandler) constructor.newInstance(manager));
			assertEquals(provider, config.customIconProvider());
		}
	}

	@Test
	public void themeIsAccessibleThroughTheRealRuneliteConfigurationProxy() throws Exception
	{
		ConfigManager manager = mock(ConfigManager.class, CALLS_REAL_METHODS);
		doReturn(null).when(manager).getConfiguration(eq(ImprovedMenusConfig.GROUP), anyString());
		Constructor<?> constructor = Class.forName("net.runelite.client.config.ConfigInvocationHandler")
				.getDeclaredConstructor(ConfigManager.class);
		constructor.setAccessible(true);
		ImprovedMenusConfig config = (ImprovedMenusConfig) Proxy.newProxyInstance(
				ImprovedMenusConfig.class.getClassLoader(), new Class<?>[] { ImprovedMenusConfig.class },
				(InvocationHandler) constructor.newInstance(manager));
		assertEquals(ImprovedMenusConfig.MenuTheme.NATIVE, config.menuTheme());
		for (ImprovedMenusConfig.MenuTheme theme : ImprovedMenusConfig.MenuTheme.values())
		{
			doReturn(theme.name()).when(manager).getConfiguration(ImprovedMenusConfig.GROUP, "menuTheme");

			config = (ImprovedMenusConfig) Proxy.newProxyInstance(ImprovedMenusConfig.class.getClassLoader(),
					new Class<?>[] { ImprovedMenusConfig.class }, (InvocationHandler) constructor.newInstance(manager));
			assertEquals(theme, config.menuTheme());
		}
	}
}
