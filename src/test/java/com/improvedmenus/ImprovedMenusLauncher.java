package com.improvedmenus;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ImprovedMenusLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ImprovedMenusPlugin.class);
		RuneLite.main(args);
	}
}
