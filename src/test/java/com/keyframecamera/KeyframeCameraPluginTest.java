package com.keyframecamera;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class KeyframeCameraPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(KeyframeCameraPlugin.class);
		RuneLite.main(args);
	}
}