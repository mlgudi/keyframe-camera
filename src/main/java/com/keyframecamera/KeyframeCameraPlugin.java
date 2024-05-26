package com.keyframecamera;

import com.google.inject.Provides;
import com.keyframecamera.panel.CameraControlPanel;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.api.VarClientInt;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import static net.runelite.client.RuneLite.RUNELITE_DIR;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

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

	@Inject
	private ChatMessageManager chatMessageManager;

	Playback playback;

	@Getter
	@Setter
	private Sequence sequence;

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

		sequence = new Sequence(config);
		playback = new Playback(this, config, configManager, client, clientThread);

		panel = new CameraControlPanel(this, playback, client, config);
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

	private int getScale()
	{
		return client.getVarcIntValue(VarClientInt.CAMERA_ZOOM_FIXED_VIEWPORT);
	}

	public int add()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return -1;
		}

		sequence.add(
			new Keyframe(
				client.getCameraFocalPointX(),
				client.getCameraFocalPointY(),
				client.getCameraFocalPointZ(),
				client.getCameraFpPitch(),
				client.getCameraFpYaw() % (2 * Math.PI),
				getScale(),
				config.defaultKeyframeEase()
			)
		);

		return sequence.size() - 1;
	}

	public void duplicate(Keyframe keyframe)
	{
		if (!sequence.contains(keyframe)) return;

		sequence.add(
			new Keyframe(
				keyframe.getFocalX(),
				keyframe.getFocalY(),
				keyframe.getFocalZ(),
				keyframe.getPitch(),
				keyframe.getYaw(),
				keyframe.getScale(),
				keyframe.getEase()
			)
		);
	}

	public void overwrite(Keyframe keyframe)
	{
		if (!sequence.contains(keyframe)) return;
		keyframe.setFocalX(client.getCameraFocalPointX());
		keyframe.setFocalY(client.getCameraFocalPointY());
		keyframe.setFocalZ(client.getCameraFocalPointZ());
		keyframe.setPitch(client.getCameraFpPitch());
		keyframe.setYaw(client.getCameraFpYaw());
		keyframe.setScale(getScale());
	}

	public void delete(Keyframe keyframe)
	{
		if (!sequence.contains(keyframe)) return;
		sequence.remove(keyframe);
	}

	public boolean moveKeyframe(boolean up, Keyframe keyframe)
	{
		int index = sequence.indexOf(keyframe);
		if (index < 0 || index >= sequence.size()) {
			return false;
		}

		if (up && index == 0) {
			return false;
		}

		if (!up && index == sequence.size() - 1) {
			return false;
		}

		if (up)
		{
			sequence.swap(sequence.get(index - 1), keyframe);
		}
		else
		{
			sequence.swap(keyframe, sequence.get(index + 1));
		}

		return true;
	}

	public void setCameraToKeyframe(Keyframe keyframe)
	{
		if (client.getCameraMode() != 1) {
			client.setCameraMode(1);
		}

		clientThread.invoke(() -> {
			client.setCameraFocalPointX(keyframe.getFocalX());
			client.setCameraFocalPointY(keyframe.getFocalY());
			client.setCameraFocalPointZ(keyframe.getFocalZ());
			client.setCameraPitchTarget(Keyframe.radiansToJau(keyframe.getPitch()));
			client.setCameraYawTarget(Keyframe.radiansToJau(keyframe.getYaw()) % 2047);
			client.runScript(ScriptID.CAMERA_DO_ZOOM, keyframe.getScale(), keyframe.getScale());
		});
	}

	public boolean freeCamEnabled() { return client.getCameraMode() == 1 && client.getGameState() == GameState.LOGGED_IN; }
	public void toggleCameraMode()
	{
		clientThread.invoke(() -> {
			client.setCameraMode(client.getCameraMode() == 0 ? 1 : 0);
		});
	}

	public void save()
	{
		String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Calendar.getInstance().getTime());
		String name = timestamp + ".txt";
		boolean success = SequenceIO.save(sequence, SEQUENCE_DIR.resolve(name).toString());

		if (success)
		{
			sendChatMessage("Sequence saved:" + name);
		}
		else
		{
			sendChatMessage("Failed to save sequence.");
		}
	}

	public void wipe()
	{
		sequence = new Sequence(config);
		redrawPanel();
	}

	public void load(String name)
	{
		Path sequencePath = KeyframeCameraPlugin.SEQUENCE_DIR.resolve(name);
		sequence = SequenceIO.load(sequencePath.toString(), config);

		if (sequence == null)
		{
			sendChatMessage("Failed to load sequence.");
		}
		else
		{
			sendChatMessage("Sequence loaded: " + name);
			redrawPanel();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		if (panel == null) return;
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.LOGGED_IN) {
			redrawPanel();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN) return;
		if (prevCameraMode != client.getCameraMode())
		{
			prevCameraMode = client.getCameraMode();
			redrawPanel();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (event.getGroup().equals(KeyframeCameraConfig.GROUP) && (event.getKey().equals("playing") || event.getKey().equals("paused"))) {
			redrawPanel();
		}
	}

	public void redrawPanel() {
		SwingUtilities.invokeLater(() -> {
			panel.updatePanel();
		});
	}

	public void sendChatMessage(String message) {

		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		final String chatMessage = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT)
			.append("[Keyframe Camera] ")
			.append(ChatColorType.NORMAL)
			.append(message)
			.build();

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(chatMessage)
			.build());
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		if (client.getGameState() != GameState.LOGGED_IN) return;
		if (client.getCameraMode() != 1) return;
		if (sequence == null) return;
		playback.tick();
	}

	@Provides
	KeyframeCameraConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(KeyframeCameraConfig.class);
	}
}
