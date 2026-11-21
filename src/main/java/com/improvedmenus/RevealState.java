package com.improvedmenus;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;

@Singleton
final class RevealState
{
	@Inject
	private Client client;
	@Inject
	private ImprovedMenusConfig config;

	boolean isRevealed()
	{
		if (config.revealKey() == ImprovedMenusConfig.RevealKey.NONE || client.getCanvas() == null
				|| !client.getCanvas().hasFocus())
		{
			return false;
		}
		switch (config.revealKey())
		{
			case CONTROL:
				return client.isKeyPressed(KeyCode.KC_CONTROL);
			case SHIFT:
				return client.isKeyPressed(KeyCode.KC_SHIFT);
			case ALT:
				return client.isKeyPressed(KeyCode.KC_ALT);
			default:
				return false;
		}
	}
}
