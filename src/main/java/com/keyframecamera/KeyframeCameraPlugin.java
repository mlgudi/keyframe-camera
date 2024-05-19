package com.keyframecamera;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.*;

import com.keyframecamera.panel.CameraControlPanel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static net.runelite.client.RuneLite.RUNELITE_DIR;

@Slf4j
@PluginDescriptor(
	name = "Keyframe Camera",
	description = "Create and play camera sequences",
	tags = {"camera", "keyframe", "creator", "content"}
)
public class KeyframeCameraPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private KeyframeCameraConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Getter
	@Setter
	private CameraSequence sequence;

	private CameraControlPanel panel;
	private NavigationButton navButton;
	private static final BufferedImage ICON = ImageUtil.loadImageResource(KeyframeCameraPlugin.class, "icon.png");

	public static Path SEQUENCE_DIR = Paths.get(RUNELITE_DIR.toString(), "sequences");
	int prevCameraMode = -1;

	@Override
	protected void startUp()
	{
		configManager.setConfiguration(KeyframeCameraConfig.GROUP, "playing", false);
		configManager.setConfiguration(KeyframeCameraConfig.GROUP, "paused", false);

		if (!SEQUENCE_DIR.toFile().exists())
		{
			SEQUENCE_DIR.toFile().mkdir();
		}

		String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Calendar.getInstance().getTime());
		sequence = new CameraSequence(this, client, config, configManager, timestamp);

		panel = new CameraControlPanel(this, client, config);
		navButton = NavigationButton.builder()
				.tooltip("Keyframe Camera")
				.icon(ICON)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		if (panel == null) return;
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.LOGGED_IN) {
			updatePanel();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN) return;
		if (prevCameraMode == -1) {
			prevCameraMode = client.getCameraMode();
			updatePanel();
			return;
		}
		if (prevCameraMode != client.getCameraMode())
		{
			prevCameraMode = client.getCameraMode();
			updatePanel();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (event.getGroup().equals(KeyframeCameraConfig.GROUP) && (event.getKey().equals("playing") || event.getKey().equals("paused"))) {
			updatePanel();
		}
	}

	public void updatePanel() {
		SwingUtilities.invokeLater(() -> panel.updateUI());
	}

	public void loadSequence(String name) {
		sequence = CameraSequence.load(this, client, config, configManager, name);
		updatePanel();
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		if (client.getGameState() != GameState.LOGGED_IN) return;
		if (client.getCameraMode() != 1) return;
		if (sequence == null) return;
		sequence.tick();
	}

	@Provides
	KeyframeCameraConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(KeyframeCameraConfig.class);
	}
}
