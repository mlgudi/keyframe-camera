package com.keyframecamera;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("keyframecamera")
public interface KeyframeCameraConfig extends Config
{
	String GROUP = "keyframecamera";

	@ConfigItem(
		keyName = "defaultDuration",
		name = "Default Duration",
		description = "The default duration of keyframes (ms)",
		position = 1
	)
	default int defaultKeyframeDuration()
	{
		return 3000;
	}

	@ConfigItem(
			keyName = "defaultKeyframeEase",
			name = "Default Easing",
			description = "The default easing type for keyframes",
			position = 2
	)
	default EaseType defaultKeyframeEase()
	{
		return EaseType.SINE;
	}

	@ConfigItem(
			keyName = "loop",
			name = "Loop",
			description = "Should the camera sequence loop?",
			position = 3
	)
	default boolean loop()
	{
		return true;
	}

	@ConfigItem(
			keyName = "playing",
			name = "Playing",
			description = "Whether the camera sequence is currently playing",
			hidden = true
	)
	default boolean playing()
	{
		return false;
	}

	@ConfigItem(
			keyName = "paused",
			name = "Paused",
			description = "Whether the camera sequence is currently paused",
			hidden = true
	)
	default boolean paused()
	{
		return false;
	}
}
